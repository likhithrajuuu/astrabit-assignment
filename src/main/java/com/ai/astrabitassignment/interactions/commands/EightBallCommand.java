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
public class EightBallCommand implements SlashCommand {

    private static final String OPTION_QUESTION = "question";
    private static final int OPTION_TYPE_STRING = 3;

    private static final List<String> RESPONSES = List.of(
            "It is certain.",
            "It is decidedly so.",
            "Without a doubt.",
            "Yes definitely.",
            "You may rely on it.",
            "As I see it, yes.",
            "Most likely.",
            "Outlook good.",
            "Yes.",
            "Signs point to yes.",
            "Reply hazy, try again.",
            "Ask again later.",
            "Better not tell you now.",
            "Cannot predict now.",
            "Concentrate and ask again.",
            "Don't count on it.",
            "My reply is no.",
            "My sources say no.",
            "Outlook not so good.",
            "Very doubtful."
    );

    private final JdbcTemplate jdbcTemplate;

    public EightBallCommand(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public String name() {
        return "8ball";
    }

    @Override
    public ApplicationCommandRequest definition() {
        return ApplicationCommandRequest.builder()
                .name(name())
                .description("Ask the Magic 8-Ball a question")
                .addOption(ApplicationCommandOptionData.builder()
                        .type(OPTION_TYPE_STRING)
                        .name(OPTION_QUESTION)
                        .description("What question do you seek answers for?")
                        .required(true)
                        .build())
                .build();
    }

    @Override
    public Map<String, Object> handle(Map<String, Object> interaction) {
        String discordInteractionId = String.valueOf(interaction.get("id"));
        String question = stringOption(interaction, OPTION_QUESTION).orElse("What is the meaning of life?");

        int index = ThreadLocalRandom.current().nextInt(RESPONSES.size());
        String answer = RESPONSES.get(index);

        try {
            jdbcTemplate.update(
                    "UPDATE interaction SET status = 'PROCESSED' WHERE payload->>'id' = ?",
                    discordInteractionId
            );
        } catch (Exception e) {
            e.printStackTrace();
        }

        String replyContent = "🎱 **Question:** " + question + "\n🔮 **Answer:** " + answer;

        return Map.of(
                "type", 4,
                "data", Map.of("content", replyContent)
        );
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