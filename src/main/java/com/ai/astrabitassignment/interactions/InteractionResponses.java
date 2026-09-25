package com.ai.astrabitassignment.interactions;

import java.util.Map;

/**
 * Builders for Discord interaction response payloads.
 */
public final class InteractionResponses {

    public static final int PONG = 1;
    public static final int CHANNEL_MESSAGE_WITH_SOURCE = 4;
    private static final int EPHEMERAL_FLAG = 64;

    private InteractionResponses() {
    }

    public static Map<String, Object> pong() {
        return Map.of("type", PONG);
    }

    public static Map<String, Object> ephemeralMessage(String content) {
        return Map.of(
                "type", CHANNEL_MESSAGE_WITH_SOURCE,
                "data", Map.of(
                        "content", content,
                        "flags", EPHEMERAL_FLAG
                )
        );
    }
}
