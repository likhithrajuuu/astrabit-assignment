package com.ai.astrabitassignment.interactions.commands;

import com.ai.astrabitassignment.interactions.InteractionResponses;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Routes an APPLICATION_COMMAND interaction to the {@link SlashCommand} registered
 * under its command name.
 */
@Component
public class SlashCommandDispatcher {

    private final Map<String, SlashCommand> commandsByName;

    public SlashCommandDispatcher(List<SlashCommand> commands) {
        this.commandsByName = commands.stream()
                .collect(Collectors.toUnmodifiableMap(SlashCommand::name, Function.identity()));
    }

    public Map<String, Object> dispatch(Map<String, Object> interaction) {
        String commandName = commandName(interaction);
        SlashCommand command = commandName != null ? commandsByName.get(commandName) : null;

        if (command == null) {
            return InteractionResponses.ephemeralMessage("Unknown command.");
        }
        return command.handle(interaction);
    }

    private String commandName(Map<String, Object> interaction) {
        if (interaction.get("data") instanceof Map<?, ?> data && data.get("name") != null) {
            return String.valueOf(data.get("name"));
        }
        return null;
    }
}
