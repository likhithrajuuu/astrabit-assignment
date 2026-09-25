package com.ai.astrabitassignment.interactions;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/interaction")
public class InteractionController {
    @PostMapping
    public Map<String, Object> receiveInteraction(
            @RequestBody Map<String, Object> interaction
    ) {
        Object type = interaction.get("type");
        if (type instanceof Number number && number.intValue() == 1) {
            return Map.of("type", 1);
        }
        return Map.of("type", 1);
    }
}
