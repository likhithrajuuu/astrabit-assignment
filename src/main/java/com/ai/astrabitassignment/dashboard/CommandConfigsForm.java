package com.ai.astrabitassignment.dashboard;

import com.ai.astrabitassignment.entities.CommandConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Form-backing wrapper so all of a guild's command configs can be edited
 * and saved together in one POST, bound via Thymeleaf's indexed-list
 * {@code th:field="*{configs[__${iter.index}__].xyz}"} syntax.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommandConfigsForm {
    private List<CommandConfig> configs;
}
