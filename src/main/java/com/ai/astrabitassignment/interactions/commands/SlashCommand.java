package com.ai.astrabitassignment.interactions.commands;

import discord4j.discordjson.json.ApplicationCommandRequest;

import java.util.Map;

/**
 * A Discord slash command: its registration definition and its interaction handler.
 * Spring collects every bean implementing this interface to both register commands
 * with Discord on startup and route incoming APPLICATION_COMMAND interactions.
 */
public interface SlashCommand {

    String name();

    ApplicationCommandRequest definition();

    Map<String, Object> handle(Map<String, Object> interaction);
}
