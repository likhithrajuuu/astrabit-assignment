package com.ai.astrabitassignment.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommandConfigId implements Serializable {
    private String guildId;
    private String commandName;
}
