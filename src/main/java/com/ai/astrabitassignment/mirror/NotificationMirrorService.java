package com.ai.astrabitassignment.mirror;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class NotificationMirrorService {

    private final String mirrorWebhookUrl;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    public NotificationMirrorService(@Value("${discord.mirror-webhook-url}") String mirrorWebhookUrl) {
        this.mirrorWebhookUrl = mirrorWebhookUrl;
    }

    public void mirrorCommandExecution(String commandName, String user, String details, String severity) {
        if (mirrorWebhookUrl == null || mirrorWebhookUrl.isBlank()) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> embed = Map.of(
                        "title", "🚨 Interaction Audit: /" + commandName,
                        "description", details != null ? details : "No details provided",
                        "color", getColorBySeverity(severity),
                        "timestamp", Instant.now().toString(),
                        "fields", List.of(
                                Map.of("name", "User", "value", user != null ? user : "Unknown", "inline", true),
                                Map.of("name", "Severity", "value", severity != null ? severity : "INFO", "inline", true)
                        )
                );

                Map<String, Object> payload = Map.of(
                        "username", "Astrabot Audit Logger",
                        "embeds", List.of(embed)
                );

                String jsonBody = jsonMapper.writeValueAsString(payload);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(mirrorWebhookUrl))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 400) {
                    System.err.println("Mirror notification failed with status: " + response.statusCode());
                }
            } catch (Exception e) {
                System.err.println("Error dispatching mirror notification: " + e.getMessage());
            }
        });
    }

    private int getColorBySeverity(String severity) {
        if (severity == null) return 0x3498DB;
        return switch (severity.toUpperCase()) {
            case "HIGH", "CRITICAL" -> 0xE74C3C;
            case "MEDIUM" -> 0xF1C40F;
            default -> 0x2ECC71;
        };
    }
}