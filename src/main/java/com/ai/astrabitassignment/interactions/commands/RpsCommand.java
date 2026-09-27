package com.ai.astrabitassignment.interactions.commands;

import discord4j.discordjson.json.ApplicationCommandRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class RpsCommand implements SlashCommand {

    @Override
    public String name() {
        return "rps";
    }

    @Override
    public ApplicationCommandRequest definition() {
        return ApplicationCommandRequest.builder()
                .name(name())
                .description("Play Rock, Paper, Scissors against the bot")
                .build();
    }

    @Override
    public Map<String, Object> handle(Map<String, Object> interaction) {
        return Map.of(
                "type", 4,
                "data", Map.of(
                        "content", "🎮 **Rock, Paper, Scissors!** Choose your weapon:",
                        "components", List.of(
                                Map.of(
                                        "type", 1, // Action Row
                                        "components", List.of(
                                                Map.of("type", 2, "style", 1, "label", "🪨 Rock", "custom_id", "rps_rock"),
                                                Map.of("type", 2, "style", 2, "label", "📄 Paper", "custom_id", "rps_paper"),
                                                Map.of("type", 2, "style", 4, "label", "✂️ Scissors", "custom_id", "rps_scissors")
                                        )
                                )
                        )
                )
        );
    }
}