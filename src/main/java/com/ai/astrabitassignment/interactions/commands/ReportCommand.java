package com.ai.astrabitassignment.interactions.commands;

import com.ai.astrabitassignment.interactions.InteractionResponses;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * {@code /report} - lets a guild member flag a message or situation for
 * moderator review. This is the seed command for the triage pipeline:
 * persistence, rule matching and AI severity scoring land in later phases.
 */
@Component
public class ReportCommand implements SlashCommand {

    private static final String OPTION_DETAILS = "details";
    private static final int OPTION_TYPE_STRING = 3;

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
        String details = stringOption(interaction, OPTION_DETAILS).orElse("(no details provided)");
        String reporter = invokingUsername(interaction).orElse("there");

        String content = "Thanks %s - your report has been received and will be reviewed:\n> %s"
                .formatted(reporter, details);

        return InteractionResponses.ephemeralMessage(content);
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
}
