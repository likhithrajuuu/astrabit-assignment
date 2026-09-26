package com.ai.astrabitassignment.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "interaction")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Interaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "guild_id", nullable = false, length = 32)
    private String guildId;

    @Column(name = "type", nullable = false, length = 50)
    private String type;

    @Column(name = "command_name", length = 100)
    private String commandName;

    @Column(name = "user_id", length = 32)
    private String userId;

    @Column(name = "user_name", length = 255)
    private String userName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    private String payload;

    @Column
    private Integer severity;

    @Column(name = "ai_summary")
    private String aiSummary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ai_tags", columnDefinition = "jsonb")
    private String aiTags;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;
}