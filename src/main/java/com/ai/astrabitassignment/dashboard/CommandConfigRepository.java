package com.ai.astrabitassignment.dashboard;

import com.ai.astrabitassignment.entities.CommandConfig;
import com.ai.astrabitassignment.entities.CommandConfigId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommandConfigRepository extends JpaRepository<CommandConfig, CommandConfigId> {
    List<CommandConfig> findByGuildId(String guildId);
}
