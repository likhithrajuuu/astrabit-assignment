package com.ai.astrabitassignment.integrations;

import discord4j.core.DiscordClient;
import org.springframework.stereotype.Component;

@Component
public class DiscordApiClient {

    private final DiscordClient discordClient;

    public DiscordApiClient(DiscordClient discordClient) {
        this.discordClient = discordClient;
    }
}