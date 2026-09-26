package com.ai.astrabitassignment.interactions.commands;


import com.ai.astrabitassignment.entities.AiTriageResult;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * {@code /report} - lets a guild member flag a message or situation for
 * moderator review. This is the seed command for the triage pipeline:
 * persistence, rule matching and AI severity scoring land in later phases.
 */
@Component
public class ReportCommand implements SlashCommand {

    @Value("${spring.ai.google.genai.api-key}")
    private String geminiApiKey;

    private static final String OPTION_DETAILS = "details";
    private static final int OPTION_TYPE_STRING = 3;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ChatClient chatClient;
    private final JdbcTemplate jdbcTemplate;

    public ReportCommand(ChatClient.Builder builder, JdbcTemplate jdbcTemplate) {
        this.chatClient = builder.build();
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public String name() {
        return "report";
    }

    @Override
    public ApplicationCommandRequest definition() {
        return ApplicationCommandRequest.builder()
                .name(name())
                .description("Report a message or situation for moderator review")
                .addOption(ApplicationCommandOptionData.builder()
                        .type(OPTION_TYPE_STRING)
                        .name(OPTION_DETAILS)
                        .description("What should moderators look into?")
                        .required(true)
                        .build())
                .build();
    }

    @Override
    public Map<String, Object> handle(Map<String, Object> interaction) {
        String discordInteractionId = (String) interaction.get("id");

        int rowsUpdated = jdbcTemplate.update(
                "UPDATE interaction SET status = 'PROCESSING' WHERE payload->>'id' = ? AND status = 'RECEIVED'",
                discordInteractionId
        );

        if (rowsUpdated == 0) {
            System.out.println("Duplicate request dropped for interaction: " + discordInteractionId);
            return Map.of(
                    "type", 5,
                    "data", Map.of("flags", 64)
            );
        }

        String details = stringOption(interaction, OPTION_DETAILS).orElse("(no details provided)");
        String reporterId = invokingUserId(interaction)
                .orElseThrow(() -> new IllegalStateException(
                        "Discord user ID is missing from interaction"
                ));
        String reporterUsername = invokingUsername(interaction)
                .orElseThrow(() -> new IllegalStateException(
                        "Discord username is missing from interaction"
                ));
        String token = (String) interaction.get("token");
        String applicationId = (String) interaction.get("application_id");

        CompletableFuture.runAsync(() -> {
            try {
                AiTriageResult aiResult = callGeminiAi(details);

                jdbcTemplate.update(
                        "UPDATE interaction SET ai_summary = ?, ai_tags = ?::jsonb, severity = ?, status = 'PROCESSED' WHERE payload->>'id' = ?",
                        aiResult.summary(),
                        aiResult.tagsJson(),
                        aiResult.severity(),
                        discordInteractionId
                );

                String finalMessage = "Thanks **%s** - your report was triaged:\n\n**AI Analysis:**\n> %s\n**Severity:** %s\n**Tags:** %s"
                        .formatted(reporterUsername, aiResult.summary(), aiResult.severity(), aiResult.tagsJson());

                sendDiscordFollowUp(applicationId, token, finalMessage);

            } catch (Exception e) {
                e.printStackTrace();
                try {
                    sendDiscordFollowUp(applicationId, token, "Report received, but AI analysis failed: " + e.getMessage());
                } catch (Exception fallbackException) {
                    System.err.println("CRITICAL: Failed to send fallback error message.");
                    fallbackException.printStackTrace();
                }
            }
        });

        return Map.of(
                "type", 5,
                "data", Map.of("flags", 64)
        );
    }

    private Optional<String> stringOption(Map<String, Object> interaction, String optionName) {
        if (!(interaction.get("data") instanceof Map<?, ?> data)) {
            return Optional.empty();
        }
        if (!(data.get("options") instanceof List<?> options)) {
            return Optional.empty();
        }
        for (Object option : options) {
            if (option instanceof Map<?, ?> optionMap
                    && optionName.equals(optionMap.get("name"))
                    && optionMap.get("value") != null) {
                return Optional.of(String.valueOf(optionMap.get("value")));
            }
        }
        return Optional.empty();
    }

    private Optional<String> invokingUsername(Map<String, Object> interaction) {
        Object user = interaction.get("member") instanceof Map<?, ?> member
                ? member.get("user")
                : interaction.get("user");

        if (user instanceof Map<?, ?> userMap && userMap.get("username") != null) {
            return Optional.of(String.valueOf(userMap.get("username")));
        }
        return Optional.empty();
    }

    private Optional<String> invokingUserId(Map<String, Object> interaction){
        Object user = interaction.get("member") instanceof Map<?, ?> member
                ? member.get("user")
                : interaction.get("user");

        if(user instanceof Map<?, ?> userMap && userMap.get("id") != null){
            return Optional.of(String.valueOf(userMap.get("id")));
        }

        return Optional.empty();
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
        String summary = aiNode.path("summary").asText();
        String tagsJson = aiNode.path("tags").toString();
        String severity = aiNode.path("severity").asText();

        return new AiTriageResult(summary, tagsJson, severity);
    }


    private void sendDiscordFollowUp(String applicationId, String token, String content) throws Exception {
        String url = "https://discord.com/api/v10/webhooks/" + applicationId + "/" + token + "/messages/@original";
        Map<String, String> requestBody = Map.of("content", content);
        String jsonBody = jsonMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(jsonBody)) // Must be PATCH
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new RuntimeException("Discord Follow-up failed: " + response.body());
        }
    }
}
