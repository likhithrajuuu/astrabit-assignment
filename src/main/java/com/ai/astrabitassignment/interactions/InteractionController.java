package com.ai.astrabitassignment.interactions;

import com.ai.astrabitassignment.interactions.commands.SlashCommandDispatcher;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Discord's single HTTP interactions endpoint. Every click, slash command and
 * PING Discord sends lands here; {@link DiscordSignatureFilter} has already
 * verified the request's Ed25519 signature by the time it reaches this method.
 *
 */
@RestController
@RequestMapping("/api/interaction")
public class InteractionController {

    private static final int PING = 1;
    private static final int APPLICATION_COMMAND = 2;

    private final SlashCommandDispatcher dispatcher;

    public InteractionController(SlashCommandDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @PostMapping
    public Map<String, Object> receiveInteraction(@RequestBody Map<String, Object> interaction) {
        int type = interaction.get("type") instanceof Number number ? number.intValue() : -1;

        return switch (type) {
            case PING -> InteractionResponses.pong();
            case APPLICATION_COMMAND -> dispatcher.dispatch(interaction);
            default -> InteractionResponses.ephemeralMessage("This interaction type isn't supported yet.");
        };
    }
}
