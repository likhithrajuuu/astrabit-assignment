-- ============================================================
-- Indexes
-- ============================================================

CREATE INDEX idx_rule_guild_command_priority
    ON rule (guild_id, command_name, priority);

CREATE INDEX idx_interaction_guild_received
    ON interaction (guild_id, received_at DESC);

CREATE INDEX idx_interaction_status
    ON interaction (status);

CREATE INDEX idx_interaction_user
    ON interaction (user_id);

CREATE INDEX idx_job_status_next_run
    ON job (status, next_run_at);

CREATE INDEX idx_job_interaction
    ON job (interaction_id);

CREATE INDEX idx_action_log_interaction_created
    ON action_log (interaction_id, created_at DESC);