package com.ai.astrabitassignment.dashboard;

import com.ai.astrabitassignment.entities.CommandConfig;
import com.ai.astrabitassignment.entities.Rule;
import com.ai.astrabitassignment.interactions.commands.SlashCommand;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DashboardService {

    private final CommandConfigRepository commandConfigRepository;
    private final RuleRepository ruleRepository;
    private final JdbcTemplate jdbcTemplate;
    private final List<SlashCommand> registeredCommands;

    public DashboardService(
            CommandConfigRepository commandConfigRepository,
            RuleRepository ruleRepository,
            JdbcTemplate jdbcTemplate,
            List<SlashCommand> registeredCommands
    ) {
        this.commandConfigRepository = commandConfigRepository;
        this.ruleRepository = ruleRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.registeredCommands = registeredCommands;
    }

    public boolean guildExists(String guildId) {
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM guild WHERE id = ?)",
                Boolean.class,
                guildId
        );
        return Boolean.TRUE.equals(exists);
    }

    /**
     * Every registered slash command should have a row for this guild so the
     * dashboard has something to show/toggle, even before an admin has
     * touched anything. Missing rows are created here, defaulted to enabled.
     */
    public List<CommandConfig> listCommandConfigs(String guildId) {
        List<String> existingNames = commandConfigRepository.findByGuildId(guildId)
                .stream()
                .map(CommandConfig::getCommandName)
                .toList();

        for (SlashCommand command : registeredCommands) {
            if (!existingNames.contains(command.name())) {
                CommandConfig defaults = new CommandConfig();
                defaults.setGuildId(guildId);
                defaults.setCommandName(command.name());
                defaults.setEnabled(true);
                defaults.setMirrorEnabled(false);
                defaults.setAiEnabled(false);
                defaults.setEphemeral(false);
                commandConfigRepository.save(defaults);
            }
        }

        return commandConfigRepository.findByGuildId(guildId);
    }

    public void saveAllCommandConfigs(List<CommandConfig> configs) {
        commandConfigRepository.saveAll(configs);
    }

    public List<String> listCommandNames() {
        return registeredCommands.stream().map(SlashCommand::name).sorted().toList();
    }

    public List<Rule> listRules(String guildId) {
        return ruleRepository.findByGuildIdOrderByPriorityAsc(guildId);
    }

    public void saveRule(Rule rule) {
        ruleRepository.save(rule);
    }

    public void deleteRule(Long ruleId) {
        ruleRepository.deleteById(ruleId);
    }
}
