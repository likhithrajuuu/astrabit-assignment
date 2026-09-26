package com.ai.astrabitassignment.interactions;

import com.ai.astrabitassignment.entities.Interaction;
import discord4j.core.object.entity.Guild;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Map;

@Service
public class InteractionService {

    private final InteractionRepository repository;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager em;

    public InteractionService(
            InteractionRepository repository,
            ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public  Interaction save(Map<String, Object> interaction) {
        String guildId = extractGuildId(interaction);
        String discordUserId = extractUserId(interaction);
        String discordUserName = extractUserName(interaction);
//        String safeOwnerId = (userId != null) ? userId : "UNKNOWN_OWNER";
        if (guildId != null && discordUserId != null) {

            // 1. Upsert the App User (with a dummy password) and get their internal BIGINT id
            // We use discord_user_id as the unique constraint check
            Long internalAppUserId = (Long) em.createNativeQuery(
                            "INSERT INTO app_user (username, password_hash, discord_user_id) " +
                                    "VALUES (:username, 'auto_generated_no_login', :discordId) " +
                                    "ON CONFLICT (discord_user_id) DO UPDATE SET username = EXCLUDED.username " +
                                    "RETURNING id"
                    )
                    .setParameter("username", discordUserName != null ? discordUserName : "Unknown")
                    .setParameter("discordId", discordUserId)
                    .getSingleResult();

            // 2. Upsert the Guild using the valid internal App User ID
            em.createNativeQuery(
                            "INSERT INTO guild (id, name, owner_user_id) " +
                                    "VALUES (:guildId, 'Auto-provisioned Guild', :ownerId) " +
                                    "ON CONFLICT (id) DO NOTHING"
                    )
                    .setParameter("guildId", guildId)
                    .setParameter("ownerId", internalAppUserId)
                    .executeUpdate();
        }
        Interaction entity = new Interaction();

        entity.setGuildId(guildId);
        entity.setType(extractType(interaction));
        entity.setCommandName(extractCommandName(interaction));
        entity.setUserId(extractUserId(interaction));
        entity.setUserName(extractUserName(interaction));
        entity.setPayload(toJson(interaction));
        entity.setStatus("RECEIVED");
        entity.setReceivedAt(Instant.now());

        return repository.save(entity);
    }

    private String extractGuildId(Map<String, Object> interaction) {
        Object guildId = interaction.get("guild_id");

        return guildId != null
                ? String.valueOf(guildId)
                : null;
    }

    private String extractType(Map<String, Object> interaction) {
        Object type = interaction.get("type");

        return type != null
                ? String.valueOf(type)
                : "UNKNOWN";
    }

    private String extractCommandName(Map<String, Object> interaction) {
        if (interaction.get("data") instanceof Map<?, ?> data
                && data.get("name") != null) {

            return String.valueOf(data.get("name"));
        }

        return null;
    }

    private String extractUserId(Map<String, Object> interaction) {
        Map<?, ?> user = extractUser(interaction);

        if (user != null && user.get("id") != null) {
            return String.valueOf(user.get("id"));
        }

        return null;
    }

    private String extractUserName(Map<String, Object> interaction) {
        Map<?, ?> user = extractUser(interaction);

        if (user != null && user.get("username") != null) {
            return String.valueOf(user.get("username"));
        }

        return null;
    }

    private Map<?, ?> extractUser(Map<String, Object> interaction) {

        if (interaction.get("member") instanceof Map<?, ?> member
                && member.get("user") instanceof Map<?, ?> user) {

            return user;
        }

        if (interaction.get("user") instanceof Map<?, ?> user) {
            return user;
        }

        return null;
    }

    private String toJson(Map<String, Object> interaction) {
        try {
            return objectMapper.writeValueAsString(interaction);
        } catch (JacksonException e) {
            throw new IllegalStateException(
                    "Failed to serialize Discord interaction",
                    e
            );
        }
    }
}