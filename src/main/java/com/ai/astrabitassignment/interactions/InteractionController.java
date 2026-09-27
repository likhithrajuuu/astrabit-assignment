package com.ai.astrabitassignment.interactions;

import com.ai.astrabitassignment.interactions.commands.ReportCommand;
import com.ai.astrabitassignment.interactions.commands.SlashCommandDispatcher;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/interaction")
public class InteractionController {

    private static final int PING = 1;
    private static final int APPLICATION_COMMAND = 2;
    private static final int MESSAGE_COMPONENT = 3;
    private static final int MODAL_SUBMIT = 5;

    private final SlashCommandDispatcher dispatcher;
    private final InteractionService interactionService;
    private final ReportCommand reportCommand;

    public InteractionController(
            SlashCommandDispatcher dispatcher,
            InteractionService interactionService,
            ReportCommand reportCommand
    ) {
        this.dispatcher = dispatcher;
        this.interactionService = interactionService;
        this.reportCommand = reportCommand;
    }

    @SuppressWarnings("unchecked")
    @PostMapping
    public Map<String, Object> receiveInteraction(
            @RequestBody Map<String, Object> interaction
    ) {
        int type = interaction.get("type") instanceof Number number
                ? number.intValue()
                : -1;

        return switch (type) {

            case PING ->
                    InteractionResponses.pong();

            case APPLICATION_COMMAND -> {
                CompletableFuture.runAsync(() -> {
                    try {
                        interactionService.save(interaction);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                yield dispatcher.dispatch(interaction);
            }

            case MESSAGE_COMPONENT -> {
                CompletableFuture.runAsync(() -> {
                    try {
                        interactionService.save(interaction);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                yield handleMessageComponent(interaction);
            }

            case MODAL_SUBMIT -> {
                CompletableFuture.runAsync(() -> {
                    try {
                        interactionService.save(interaction);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                Map<String, Object> data = (Map<String, Object>) interaction.get("data");
                String customId = (String) data.get("custom_id");

                if ("report_modal".equals(customId)) {
                    yield reportCommand.handle(interaction);
                }

                yield InteractionResponses.ephemeralMessage("Modal not recognized.");
            }

            default ->
                    InteractionResponses.ephemeralMessage(
                            "This interaction type isn't supported yet."
                    );
        };
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> handleMessageComponent(Map<String, Object> interaction) {
        Map<String, Object> data = (Map<String, Object>) interaction.get("data");
        String customId = (String) data.get("custom_id");

        if (customId != null && customId.startsWith("rps_")) {
            String userChoice = customId.substring(4);
            List<String> choices = List.of("rock", "paper", "scissors");
            String botChoice = choices.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(3));

            String result;
            if (userChoice.equals(botChoice)) {
                result = "It's a tie! 🤝";
            } else if ((userChoice.equals("rock") && botChoice.equals("scissors")) ||
                    (userChoice.equals("paper") && botChoice.equals("rock")) ||
                    (userChoice.equals("scissors") && botChoice.equals("paper"))) {
                result = "You win! 🎉";
            } else {
                result = "I win! 🤖";
            }

            String displayUser = getEmojiForChoice(userChoice) + " " + userChoice.toUpperCase();
            String displayBot = getEmojiForChoice(botChoice) + " " + botChoice.toUpperCase();

            // Type 7 instantly edits the message where the button was clicked
            return Map.of(
                    "type", 7,
                    "data", Map.of(
                            "content", "You chose " + displayUser + "\nI chose " + displayBot + "\n\n**" + result + "**",
                            "components", List.of()
                    )
            );
        }

        return InteractionResponses.ephemeralMessage("Unknown button clicked.");
    }

    private String getEmojiForChoice(String choice) {
        return switch (choice) {
            case "rock" -> "🪨";
            case "paper" -> "📄";
            case "scissors" -> "✂️";
            default -> "❓";
        };
    }
}