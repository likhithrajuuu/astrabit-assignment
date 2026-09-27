package com.ai.astrabitassignment.interactions;

import com.ai.astrabitassignment.entities.Interaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InteractionRepository extends JpaRepository<Interaction, Long> {
    Page<Interaction> findByGuildId(String guildId, Pageable pageable);

    boolean existsByDiscordInteractionId(String discordInteractionId);
}
