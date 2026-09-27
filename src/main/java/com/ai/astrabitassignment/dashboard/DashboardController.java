package com.ai.astrabitassignment.dashboard;

import com.ai.astrabitassignment.entities.CommandConfig;
import com.ai.astrabitassignment.entities.Interaction;
import com.ai.astrabitassignment.entities.Rule;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * JSON API backing the Next.js dashboard: per-guild command configuration,
 * rules, and the interaction log. Everything under here requires a signed-in
 * Google account (see {@code SecurityConfig}) - that proves who's signed in,
 * but not yet that they own the specific guild they're querying: any
 * signed-in Google account can currently read/edit any guild's data by ID.
 * Real per-guild authorization (matching the signed-in account to that
 * guild's owner) is a separate, not-yet-built step.
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private static final List<String> MATCH_TYPES = List.of("KEYWORD", "REGEX", "AI_SEVERITY");
    private static final List<String> ACTIONS = List.of("SET_SEVERITY", "MIRROR", "PING_ROLE", "ADD_BUTTONS");

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * Called by the frontend right after Discord redirects back from the
     * "Add to Server" flow with a {@code guild_id}. Auto-provisions the
     * guild so its dashboard works immediately instead of showing
     * "server hasn't connected yet" until someone runs a command.
     */
    @PostMapping("/connect")
    public Map<String, String> connect(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal OAuth2User principal
    ) {
        String guildId = body.get("guildId");
        String adminEmail = principal.getAttribute("email");
        String guildName = dashboardService.connectGuild(guildId, adminEmail);
        return Map.of("guildId", guildId, "guildName", guildName);
    }

    @GetMapping("/meta")
    public Map<String, Object> meta() {
        return Map.of(
                "commandNames", dashboardService.listCommandNames(),
                "matchTypes", MATCH_TYPES,
                "actions", ACTIONS
        );
    }

    @GetMapping("/{guildId}/commands")
    public ResponseEntity<List<CommandConfig>> commands(@PathVariable String guildId) {
        if (!dashboardService.guildExists(guildId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(dashboardService.listCommandConfigs(guildId));
    }

    @PutMapping("/{guildId}/commands")
    public ResponseEntity<Void> updateCommands(
            @PathVariable String guildId,
            @RequestBody List<CommandConfig> configs
    ) {
        configs.forEach(config -> config.setGuildId(guildId));
        dashboardService.saveAllCommandConfigs(configs);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{guildId}/rules")
    public ResponseEntity<List<Rule>> rules(@PathVariable String guildId) {
        if (!dashboardService.guildExists(guildId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(dashboardService.listRules(guildId));
    }

    @PostMapping("/{guildId}/rules")
    public ResponseEntity<Rule> addRule(@PathVariable String guildId, @RequestBody Rule rule) {
        rule.setId(null);
        rule.setGuildId(guildId);
        if (rule.getParams() == null || rule.getParams().isBlank()) {
            rule.setParams("{}");
        }
        return ResponseEntity.ok(dashboardService.saveRule(rule));
    }

    @DeleteMapping("/{guildId}/rules/{ruleId}")
    public ResponseEntity<Void> deleteRule(@PathVariable String guildId, @PathVariable Long ruleId) {
        dashboardService.deleteRule(ruleId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{guildId}/interactions")
    public ResponseEntity<Page<Interaction>> interactions(
            @PathVariable String guildId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (!dashboardService.guildExists(guildId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(dashboardService.listInteractions(guildId, page, size));
    }
}
