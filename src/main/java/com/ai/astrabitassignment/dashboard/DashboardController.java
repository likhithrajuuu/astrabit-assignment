package com.ai.astrabitassignment.dashboard;

import com.ai.astrabitassignment.entities.Rule;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * A minimal server-rendered dashboard for guild admins to make command
 * behavior configurable instead of hardcoded: enable/disable a command,
 * customize its reply, toggle mirroring/AI, and define keyword/regex/AI
 * severity rules against the {@code rule} table.
 * <p>
 * No authentication yet - anyone who knows a guild ID can open its
 * dashboard. That's fine for local development but not for a real
 * deployment; it needs to be gated on real Discord OAuth + guild ownership
 * before this goes anywhere public.
 */
@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private static final java.util.List<String> MATCH_TYPES = java.util.List.of("KEYWORD", "REGEX", "AI_SEVERITY");
    private static final java.util.List<String> ACTIONS = java.util.List.of("SET_SEVERITY", "MIRROR", "PING_ROLE", "ADD_BUTTONS");

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public String landing() {
        return "dashboard/index";
    }

    @GetMapping("/go")
    public String go(@RequestParam String guildId) {
        return "redirect:/dashboard/" + guildId + "/commands";
    }

    @GetMapping("/{guildId}/commands")
    public String commands(@PathVariable String guildId, Model model) {
        if (!dashboardService.guildExists(guildId)) {
            model.addAttribute("guildId", guildId);
            return "dashboard/guild-not-found";
        }

        model.addAttribute("guildId", guildId);
        model.addAttribute("form", new CommandConfigsForm(dashboardService.listCommandConfigs(guildId)));
        return "dashboard/commands";
    }

    @PostMapping("/{guildId}/commands")
    public String updateCommands(@PathVariable String guildId, @ModelAttribute CommandConfigsForm form) {
        form.getConfigs().forEach(config -> config.setGuildId(guildId));
        dashboardService.saveAllCommandConfigs(form.getConfigs());
        return "redirect:/dashboard/" + guildId + "/commands";
    }

    @GetMapping("/{guildId}/rules")
    public String rules(@PathVariable String guildId, Model model) {
        if (!dashboardService.guildExists(guildId)) {
            model.addAttribute("guildId", guildId);
            return "dashboard/guild-not-found";
        }

        model.addAttribute("guildId", guildId);
        model.addAttribute("rules", dashboardService.listRules(guildId));
        model.addAttribute("matchTypes", MATCH_TYPES);
        model.addAttribute("actions", ACTIONS);
        model.addAttribute("commandNames", dashboardService.listCommandNames());
        model.addAttribute("newRule", new Rule());
        return "dashboard/rules";
    }

    @PostMapping("/{guildId}/rules")
    public String addRule(@PathVariable String guildId, @ModelAttribute("newRule") Rule rule) {
        rule.setId(null);
        rule.setGuildId(guildId);
        if (rule.getParams() == null || rule.getParams().isBlank()) {
            rule.setParams("{}");
        }
        dashboardService.saveRule(rule);
        return "redirect:/dashboard/" + guildId + "/rules";
    }

    @PostMapping("/{guildId}/rules/{ruleId}/delete")
    public String deleteRule(@PathVariable String guildId, @PathVariable Long ruleId) {
        dashboardService.deleteRule(ruleId);
        return "redirect:/dashboard/" + guildId + "/rules";
    }
}
