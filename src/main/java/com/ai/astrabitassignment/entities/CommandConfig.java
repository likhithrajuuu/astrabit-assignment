package com.ai.astrabitassignment.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "command_config")
@IdClass(CommandConfigId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommandConfig {

    @Id
    @Column(name = "guild_id", length = 32)
    private String guildId;

    @Id
    @Column(name = "command_name", length = 100)
    private String commandName;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "reply_template")
    private String replyTemplate;

    @Column(name = "mirror_enabled", nullable = false)
    private Boolean mirrorEnabled = false;

    @Column(name = "ai_enabled", nullable = false)
    private Boolean aiEnabled = false;

    @Column(nullable = false)
    private Boolean ephemeral = false;
}
