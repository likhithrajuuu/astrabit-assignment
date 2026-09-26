package com.ai.astrabitassignment.interactions.commands;

import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class RollCommand implements SlashCommand {

    private static final String OPTION_SIDES = "sides";
    private static final int OPTION_TYPE_INTEGER = 4;

    private final JdbcTemplate jdbcTemplate;

    public RollCommand(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public String name() {
        return "roll";
    }

    @Override
    public ApplicationCommandRequest definition() {
        return ApplicationCommandRequest.builder()
                .name(name())
                .description("Roll a die or a random number")
                .addOption(ApplicationCommandOptionData.builder()
                        .type(OPTION_TYPE_INTEGER)
                        .name(OPTION_SIDES)
                        .description("Number of sides (default is 6)")
                        .required(false)
                        .build())
                .build();
    }

    @Override
    public Map<String, Object> handle(Map<String, Object> interaction) {
        String discordInteractionId = String.valueOf(interaction.get("id"));
        long sides = integerOption(interaction, OPTION_SIDES).orElse(6L);

        if (sides < 2) {
            sides = 2;
        } else if (sides > 1000000) {
            sides = 1000000;
        }

        long roll = ThreadLocalRandom.current().nextLong(1, sides + 1);

        try {
            jdbcTemplate.update(
                    "UPDATE interaction SET status = 'PROCESSED' WHERE payload->>'id' = ?",
                    discordInteractionId
            );
        } catch (Exception e) {
            e.printStackTrace();
        }

        String message = "🎲 You rolled a **" + roll + "** (1-" + sides + ")!";

        return Map.of(
                "type", 4,
                "data", Map.of("content", message)
        );
    }

    private Optional<Long> integerOption(Map<String, Object> interaction, String optionName) {
        if (!(interaction.get("data") instanceof Map<?, ?> data)) return Optional.empty();
        if (!(data.get("options") instanceof List<?> options)) return Optional.empty();
        for (Object option : options) {
            if (option instanceof Map<?, ?> optionMap && optionName.equals(optionMap.get("name")) && optionMap.get("value") != null) {
                try {
                    return Optional.of(Long.parseLong(String.valueOf(optionMap.get("value"))));
                } catch (NumberFormatException ignored) {
                    return Optional.empty();
                }
            }
        }
        return Optional.empty();
    }
}