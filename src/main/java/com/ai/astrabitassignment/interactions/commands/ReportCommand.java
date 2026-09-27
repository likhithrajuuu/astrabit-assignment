package com.ai.astrabitassignment.interactions.commands;

import com.ai.astrabitassignment.interactions.InteractionResponses;
import com.ai.astrabitassignment.services.ReportProcessingService;
import discord4j.discordjson.json.ApplicationCommandRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ReportCommand implements SlashCommand {

    private static final Logger log = LoggerFactory.getLogger(ReportCommand.class);
    private final ReportProcessingService processingService;

    public ReportCommand(ReportProcessingService processingService) {
        this.processingService = processingService;
    }

    @Override
    public String name() {
        return "report";
    }

    @Override
    public ApplicationCommandRequest definition() {
        return ApplicationCommandRequest.builder()
                .name(name())
                .description("Open a form to report a message or situation to moderators")
                .build();
    }

    @Override
    public Map<String, Object> handle(Map<String, Object> interaction) {
        // Extract interaction type safely
        int type = interaction.get("type") instanceof Number number
                ? number.intValue()
                : -1;

        if (type == 2) { // APPLICATION_COMMAND (/report)
            log.info("Received /report command; opening modal.");
            return openReportModal();
        } else if (type == 5) { // MODAL_SUBMIT
            log.info("Received report_modal submission; processing asynchronously.");
            return processModalSubmission(interaction);
        }

        log.warn("ReportCommand received unsupported interaction type: {}", type);
        return InteractionResponses.ephemeralMessage("Unsupported interaction type.");
    }

    private Map<String, Object> openReportModal() {
        return Map.of(
                "type", 9, // MODAL
                "data", Map.of(
                        "title", "Submit a Moderation Report",
                        "custom_id", "report_modal",
                        "components", List.of(
                                Map.of(
                                        "type", 1, // ACTION_ROW
                                        "components", List.of(
                                                Map.of(
                                                        "type", 4, // TEXT_INPUT
                                                        "custom_id", "report_details",
                                                        "label", "What happened?",
                                                        "style", 2, // PARAGRAPH
                                                        "placeholder", "Please provide context or specific quotes...",
                                                        "required", true,
                                                        "min_length", 10,
                                                        "max_length", 1500
                                                )
                                        )
                                )
                        )
                )
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> processModalSubmission(Map<String, Object> interaction) {
        String discordInteractionId = (String) interaction.get("id");
        String reporterUsername = extractUsername(interaction);
        String token = (String) interaction.get("token");
        String applicationId = (String) interaction.get("application_id");

        // Extract value typed into "report_details"
        Map<String, Object> data = (Map<String, Object>) interaction.get("data");
        List<Map<String, Object>> components = (List<Map<String, Object>>) data.get("components");
        String details = "(no details provided)";

        try {
            // Traverse structure: ActionRow -> TextInput
            Map<String, Object> actionRow = components.get(0);
            List<Map<String, Object>> innerComponents = (List<Map<String, Object>>) actionRow.get("components");
            Map<String, Object> textInput = innerComponents.get(0);
            details = (String) textInput.get("value");
        } catch (Exception e) {
            log.error("Failed to extract report_details value from modal submission.", e);
        }
        processingService.processReportAsync(discordInteractionId, details, reporterUsername, applicationId, token);
        return Map.of("type", 5);
    }

    private String extractUsername(Map<String, Object> interaction) {
        Object user = interaction.get("member") instanceof Map<?, ?> member
                ? member.get("user")
                : interaction.get("user");

        if (user instanceof Map<?, ?> userMap && userMap.get("username") != null) {
            return String.valueOf(userMap.get("username"));
        }
        return "Unknown User";
    }
}