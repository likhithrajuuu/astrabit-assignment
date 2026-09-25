package com.ai.astrabitassignment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "discord")
public record DiscordProperties(
        String appId,
        String publicKey,
        String botToken
){

}
