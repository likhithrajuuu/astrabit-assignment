package com.ai.astrabitassignment.services;

import com.ai.astrabitassignment.entities.AiTriageResult;
import com.ai.astrabitassignment.mirror.NotificationMirrorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class ReportProcessingService {

    private static final Logger log = LoggerFactory.getLogger(ReportProcessingService.class);

    private final ChatClient chatClient;
    private final JdbcTemplate jdbcTemplate;
    private final NotificationMirrorService mirrorService;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public ReportProcessingService(ChatClient.Builder builder, JdbcTemplate jdbcTemplate, NotificationMirrorService mirrorService) {
        this.chatClient = builder.build();
        this.jdbcTemplate = jdbcTemplate;
        this.mirrorService = mirrorService;
    }

    public void processReportAsync(String discordInteractionId, String details, String reporterUsername, String applicationId, String token) {
        log.info("Initiated async report processing for interaction ID: {}", discordInteractionId);

        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(2000);

                log.debug("Attempting to acquire deduplication lock for interaction ID: {}", discordInteractionId);

                int rowsUpdated = jdbcTemplate.update(
                        "UPDATE interaction SET status = 'PROCESSING' WHERE payload->>'id' = ? AND status = 'RECEIVED'",
                        discordInteractionId
                );

                if (rowsUpdated == 0) {
                    log.warn("ABORTED: Interaction ID {} was already processed, or the initial INSERT hasn't finished yet.", discordInteractionId);
                    return;
                }

                log.info("Lock acquired. Calling Gemini AI for interaction ID: {}", discordInteractionId);
                AiTriageResult aiResult = callGeminiAi(details);
                log.info("Gemini AI triaged successfully. Severity: {}", aiResult.severity());

                jdbcTemplate.update(
                        "UPDATE interaction SET ai_summary = ?, ai_tags = ?::jsonb, severity = ?, status = 'PROCESSED' WHERE payload->>'id' = ?",
                        aiResult.summary(),
                        aiResult.tagsJson(),
                        aiResult.severity(),
                        discordInteractionId
                );
                log.debug("Database updated to PROCESSED for interaction ID: {}", discordInteractionId);

                String finalMessage = "Thanks **%s** - your report was triaged:\n\n**AI Analysis:**\n> %s\n**Severity:** %s\n**Tags:** %s"
                        .formatted(reporterUsername, aiResult.summary(), aiResult.severity(), aiResult.tagsJson());

                sendDiscordFollowUp(applicationId, token, finalMessage);
                log.info("Successfully sent Discord follow-up message for interaction ID: {}", discordInteractionId);

                mirrorService.mirrorCommandExecution(
                        "report",
                        reporterUsername,
                        "**AI Summary:** " + aiResult.summary() + "\n**Report Details:** " + details,
                        aiResult.severity()
                );
                log.info("Mirror webhook dispatched successfully.");

            } catch (Exception e) {
                log.error("Failed to process report for interaction ID: {}", discordInteractionId, e);
                try {
                    sendDiscordFollowUp(applicationId, token, "Report received, but AI analysis failed: " + e.getMessage());
                } catch (Exception fallback) {
                    log.error("CRITICAL: Discord fallback message also failed for interaction ID: {}", discordInteractionId, fallback);
                }
            }
        });
    }

    private AiTriageResult callGeminiAi(String userDetails) throws Exception {
        String promptText = """
            You are a moderation bot. Analyze this report: "%s"
            Return ONLY a JSON object (no markdown, no backticks) with these exact keys:
            "summary" (string, 1 sentence max)
            "tags" (array of strings, 2-3 single-word tags)
            "severity" (string, choose one: "1 - Minor", "2 - Low", "3 - Moderate", "4 - High", "5 - Critical")
            """.formatted(userDetails);

        String rawAiResponse = chatClient.prompt(promptText).call().content();
        String cleanJson = rawAiResponse.replaceAll("(?s)^```json\\s*|\\s*```$", "").trim();

        JsonNode aiNode = jsonMapper.readTree(cleanJson);
        return new AiTriageResult(
                aiNode.path("summary").asText(),
                aiNode.path("tags").toString(),
                aiNode.path("severity").asText()
        );
    }

    private void sendDiscordFollowUp(String applicationId, String token, String content) throws Exception {
        String url = "https://discord.com/api/v10/webhooks/" + applicationId + "/" + token + "/messages/@original";
        String jsonBody = jsonMapper.writeValueAsString(Map.of("content", content));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new RuntimeException("Discord API returned status " + response.statusCode() + ": " + response.body());
        }
    }
}