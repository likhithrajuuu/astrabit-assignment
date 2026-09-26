package com.ai.astrabitassignment.interactions.commands;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Component
public class AskCommand implements SlashCommand {

    private static final String OPTION_QUESTION = "question";
    private static final int OPTION_TYPE_STRING = 3;

    private final ChatClient chatClient;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final JdbcTemplate jdbcTemplate;

    public AskCommand(ChatClient.Builder chatClientBuilder, JdbcTemplate jdbcTemplate) {
        this.chatClient = chatClientBuilder.build();
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public String name() {
        return "ask";
    }

    @Override
    public ApplicationCommandRequest definition() {
        return ApplicationCommandRequest.builder()
                .name(name())
                .description("Ask anything to Astrabot")
                .addOption(ApplicationCommandOptionData.builder()
                        .type(OPTION_TYPE_STRING)
                        .name(OPTION_QUESTION)
                        .description("What do you want to ask?")
                        .required(true)
                        .build())
                .build();
    }

    @Override
    public Map<String, Object> handle(Map<String, Object> interaction) {
        String discordInteractionId = String.valueOf(interaction.get("id"));
        String question = stringOption(interaction, OPTION_QUESTION).orElse("Hello?");
        String token = String.valueOf(interaction.get("token"));
        String applicationId = String.valueOf(interaction.get("application_id"));

        CompletableFuture.runAsync(() -> {
            try {

                //Discord uses 2000 characters restriction
                String aiResponse = chatClient.prompt()
                        .system("You are Astrabot, a highly capable, sharp, and witty AI assistant. Provide engaging and direct answers. You must keep your response strictly under 1900 characters.")
                        .user(question)
                        .call()
                        .content();

                if (aiResponse != null && aiResponse.length() > 2000) {
                    aiResponse = aiResponse.substring(0, 1997) + "...";
                }

                jdbcTemplate.update(
                        "UPDATE interaction SET status = 'PROCESSED' WHERE payload->>'id' = ?",
                        discordInteractionId
                );

                sendDiscordFollowUp(applicationId, token, aiResponse);

            } catch (Exception e) {
                e.printStackTrace();
                try {
                    sendDiscordFollowUp(applicationId, token, "I encountered an error trying to think about that.");
                } catch (Exception fallbackException) {
                    fallbackException.printStackTrace();
                }
            }
        });

        // Explicitly format the Type 5 response so Discord doesn't reject it
        return Map.of(
                "type", 5,
                "data", Map.of()
        );
    }

    private void sendDiscordFollowUp(String applicationId, String token, String content) throws Exception {
        String url = "https://discord.com/api/v10/webhooks/" + applicationId + "/" + token + "/messages/@original";
        Map<String, String> requestBody = Map.of("content", content);
        String jsonBody = jsonMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new RuntimeException("Discord Follow-up failed: " + response.body());
        }
    }

    private Optional<String> stringOption(Map<String, Object> interaction, String optionName) {
        if (!(interaction.get("data") instanceof Map<?, ?> data)) return Optional.empty();
        if (!(data.get("options") instanceof List<?> options)) return Optional.empty();
        for (Object option : options) {
            if (option instanceof Map<?, ?> optionMap && optionName.equals(optionMap.get("name")) && optionMap.get("value") != null) {
                return Optional.of(String.valueOf(optionMap.get("value")));
            }
        }
        return Optional.empty();
    }
}