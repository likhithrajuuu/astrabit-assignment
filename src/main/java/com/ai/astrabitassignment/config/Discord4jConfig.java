package com.ai.astrabitassignment.config;

import com.ai.astrabitassignment.security.Ed25519Verifier;
import discord4j.core.DiscordClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Discord4jConfig {

    @Bean
    public DiscordClient discordClient(
            DiscordProperties discordProperties
    ) {
        return DiscordClient.create(
                discordProperties.botToken()
        );
    }

    @Bean
    public Ed25519Verifier ed25519Verifier(
            DiscordProperties discordProperties
    ) {
        return new Ed25519Verifier(
                discordProperties.publicKey()
        );
    }
}