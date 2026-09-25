package com.ai.astrabitassignment.interactions.commands;

import com.ai.astrabitassignment.config.DiscordProperties;
import discord4j.core.DiscordClient;
import discord4j.discordjson.json.ApplicationCommandRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Bulk-overwrites Discord's global slash-command registration with the
 * {@link SlashCommand} beans present in the application context. Bulk
 * overwrite is idempotent, so this is safe to run on every startup.
 * <p>
 * Registration failures (e.g. no network, placeholder credentials in a local
 * dev profile) are logged and swallowed rather than failing application
 * startup - the HTTP interactions endpoint itself doesn't depend on it.
 */
@Component
public class DiscordCommandRegistrar implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DiscordCommandRegistrar.class);

    private final DiscordClient discordClient;
    private final DiscordProperties discordProperties;
    private final List<SlashCommand> commands;

    public DiscordCommandRegistrar(
            DiscordClient discordClient,
            DiscordProperties discordProperties,
            List<SlashCommand> commands
    ) {
        this.discordClient = discordClient;
        this.discordProperties = discordProperties;
        this.commands = commands;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (commands.isEmpty()) {
            return;
        }

        List<ApplicationCommandRequest> definitions = commands.stream()
                .map(SlashCommand::definition)
                .toList();

        try {
            long applicationId = Long.parseLong(discordProperties.appId());
            discordClient.getApplicationService()
                    .bulkOverwriteGlobalApplicationCommand(applicationId, definitions)
                    .doOnNext(registered -> log.info("Registered Discord command: {}", registered.name()))
                    .then()
                    .block();
        } catch (Exception e) {
            log.warn("Skipping Discord slash-command registration: {}", e.getMessage());
        }
    }
}
