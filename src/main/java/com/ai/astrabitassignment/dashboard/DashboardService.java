package com.ai.astrabitassignment.dashboard;

import com.ai.astrabitassignment.entities.CommandConfig;
import com.ai.astrabitassignment.entities.Interaction;
import com.ai.astrabitassignment.entities.Rule;
import com.ai.astrabitassignment.interactions.InteractionRepository;
import com.ai.astrabitassignment.interactions.commands.SlashCommand;
import discord4j.core.DiscordClient;
import discord4j.discordjson.json.GuildUpdateData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

    private final CommandConfigRepository commandConfigRepository;
    private final RuleRepository ruleRepository;
    private final InteractionRepository interactionRepository;
    private final JdbcTemplate jdbcTemplate;
    private final List<SlashCommand> registeredCommands;
    private final DiscordClient discordClient;

    public DashboardService(
            CommandConfigRepository commandConfigRepository,
            RuleRepository ruleRepository,
            InteractionRepository interactionRepository,
            JdbcTemplate jdbcTemplate,
            List<SlashCommand> registeredCommands,
            DiscordClient discordClient
    ) {
        this.commandConfigRepository = commandConfigRepository;
        this.ruleRepository = ruleRepository;
        this.interactionRepository = interactionRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.registeredCommands = registeredCommands;
        this.discordClient = discordClient;
    }

    /**
     * Called right after an admin finishes Discord's "Add to Server" OAuth
     * flow. Auto-provisions the {@code app_user}/{@code guild} rows (the
     * same pattern {@code InteractionService} uses for guilds discovered via
     * a slash command, just triggered by the OAuth callback instead of
     * waiting for the first command) and seeds default command configs, so
     * the dashboard has something to show immediately - no need to go run
     * /ping in Discord first.
     */
    public String connectGuild(String guildId, String adminEmail) {
        Long appUserId = findOrCreateDashboardAppUser(adminEmail);

        String guildName = fetchGuildName(guildId).orElse("Connected Server");

        jdbcTemplate.update(
                "INSERT INTO guild (id, name, owner_user_id) VALUES (?, ?, ?) " +
                        "ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name",
                guildId, guildName, appUserId
        );

        listCommandConfigs(guildId);

        return guildName;
    }

    /**
     * A plain {@code ON CONFLICT (discord_user_id) DO UPDATE} isn't safe here:
     * {@code app_user} also has a separate unique constraint on
     * {@code username}, and since both columns are derived from the same
     * email, a second connect from the same admin collides on both
     * constraints at once. Postgres only resolves a conflict on the
     * exact arbiter named in ON CONFLICT - a simultaneous conflict on a
     * different constraint (username) throws instead of updating. Checking
     * first sidesteps that entirely.
     */
    private Long findOrCreateDashboardAppUser(String adminEmail) {
        String discordUserId = "dashboard:" + adminEmail;

        List<Long> existing = jdbcTemplate.query(
                "SELECT id FROM app_user WHERE discord_user_id = ?",
                (rs, rowNum) -> rs.getLong("id"),
                discordUserId
        );
        if (!existing.isEmpty()) {
            return existing.get(0);
        }

        return jdbcTemplate.queryForObject(
                "INSERT INTO app_user (username, password_hash, discord_user_id) " +
                        "VALUES (?, 'oauth_dashboard_admin', ?) RETURNING id",
                Long.class,
                adminEmail,
                discordUserId
        );
    }

    private Optional<String> fetchGuildName(String guildId) {
        try {
            GuildUpdateData guild = discordClient.getGuildService()
                    .getGuild(Long.parseLong(guildId))
                    .block();
            return Optional.ofNullable(guild).map(GuildUpdateData::name);
        } catch (Exception e) {
            log.warn("Could not fetch guild name for {} right after connecting: {}", guildId, e.getMessage());
            return Optional.empty();
        }
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

    public Rule saveRule(Rule rule) {
        return ruleRepository.save(rule);
    }

    public void deleteRule(Long ruleId) {
        ruleRepository.deleteById(ruleId);
    }

    public Page<Interaction> listInteractions(String guildId, int page, int size) {
        return interactionRepository.findByGuildId(
                guildId,
                PageRequest.of(page, size, Sort.by("receivedAt").descending())
        );
    }
}
