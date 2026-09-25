package com.ai.astrabitassignment.interactions.commands;

import discord4j.discordjson.json.ApplicationCommandRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class PingCommand implements SlashCommand{
    @Override
    public String name(){
        return "ping";
    }

    @Override
    public ApplicationCommandRequest definition(){
        return ApplicationCommandRequest.builder()
                .name("ping")
                .description("replies with a pong")
                .build();
    }
    @Override
    public Map<String, Object> handle(Map<String, Object> interaction){
        return Map.of(
                "type", 4,
                "data", Map.of(
                        "content", "pong"
                )
        );
    }
}
