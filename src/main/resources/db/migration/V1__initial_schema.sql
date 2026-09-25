-- ============================================================
-- Extensions
-- ============================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;


-- ============================================================
-- app_user
-- ============================================================

CREATE TABLE app_user (
                          id BIGSERIAL PRIMARY KEY,

                          username VARCHAR(100) NOT NULL,
                          password_hash TEXT NOT NULL,
                          discord_user_id VARCHAR(32) NOT NULL,

                          CONSTRAINT uq_app_user_username
                              UNIQUE (username),

                          CONSTRAINT uq_app_user_discord_user
                              UNIQUE (discord_user_id)
);


-- ============================================================
-- guild - primarily used by discord platform
-- ============================================================

CREATE TABLE guild (
                       id VARCHAR(32) PRIMARY KEY, -- Discord Guild ID

                       name VARCHAR(255) NOT NULL,

                       owner_user_id BIGINT NOT NULL,

                       post_channel_id VARCHAR(32),

                       mirror_webhook_enc TEXT,

                       connected_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                       CONSTRAINT fk_guild_owner
                           FOREIGN KEY (owner_user_id)
                               REFERENCES app_user(id)
                               ON DELETE RESTRICT
);


-- ============================================================
-- command_config
-- ============================================================

CREATE TABLE command_config (
                                guild_id VARCHAR(32) NOT NULL,

                                command_name VARCHAR(100) NOT NULL,

                                enabled BOOLEAN NOT NULL DEFAULT TRUE,

                                reply_template TEXT,

                                mirror_enabled BOOLEAN NOT NULL DEFAULT FALSE,

                                ai_enabled BOOLEAN NOT NULL DEFAULT FALSE,

                                ephemeral BOOLEAN NOT NULL DEFAULT FALSE,

                                PRIMARY KEY (guild_id, command_name),

                                CONSTRAINT fk_command_config_guild
                                    FOREIGN KEY (guild_id)
                                        REFERENCES guild(id)
                                        ON DELETE CASCADE
);


-- ============================================================
-- rule
-- ============================================================

CREATE TABLE rule (
                      id BIGSERIAL PRIMARY KEY,

                      guild_id VARCHAR(32) NOT NULL,

                      command_name VARCHAR(100) NOT NULL,

                      match_type VARCHAR(20) NOT NULL,

                      pattern TEXT NOT NULL,

                      action VARCHAR(30) NOT NULL,

                      params JSONB NOT NULL DEFAULT '{}'::jsonb,

                      priority INTEGER NOT NULL DEFAULT 0,

                      CONSTRAINT fk_rule_guild
                          FOREIGN KEY (guild_id)
                              REFERENCES guild(id)
                              ON DELETE CASCADE,

                      CONSTRAINT chk_rule_match_type
                          CHECK (
                              match_type IN (
                                             'KEYWORD',
                                             'REGEX',
                                             'AI_SEVERITY'
                                  )
                              ),

                      CONSTRAINT chk_rule_action
                          CHECK (
                              action IN (
                              'SET_SEVERITY',
                              'MIRROR',
                              'PING_ROLE',
                              'ADD_BUTTONS'
                              )
)
    );


-- ============================================================
-- interaction
-- ============================================================

CREATE TABLE interaction (
                             id BIGSERIAL PRIMARY KEY,

                             guild_id VARCHAR(32) NOT NULL,

                             type VARCHAR(50) NOT NULL,

                             command_name VARCHAR(100),

                             user_id VARCHAR(32),

                             user_name VARCHAR(255),

                             payload JSONB,

                             severity INTEGER,

                             ai_summary TEXT,

                             ai_tags JSONB,

                             status VARCHAR(30) NOT NULL,

                             received_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                             CONSTRAINT fk_interaction_guild
                                 FOREIGN KEY (guild_id)
                                     REFERENCES guild(id)
                                     ON DELETE CASCADE
);


-- ============================================================
-- job
-- ============================================================

CREATE TABLE job (
                     id BIGSERIAL PRIMARY KEY,

                     interaction_id BIGINT NOT NULL,

                     kind VARCHAR(100) NOT NULL,

                     status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

                     attempts INTEGER NOT NULL DEFAULT 0,

                     next_run_at TIMESTAMPTZ,

                     last_error TEXT,

                     updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                     CONSTRAINT fk_job_interaction
                         FOREIGN KEY (interaction_id)
                             REFERENCES interaction(id)
                             ON DELETE CASCADE,

                     CONSTRAINT chk_job_status
                         CHECK (
                             status IN (
                                        'PENDING',
                                        'RUNNING',
                                        'DONE',
                                        'FAILED',
                                        'DEAD'
                                 )
                             ),

                     CONSTRAINT chk_job_attempts
                         CHECK (attempts >= 0)
);


-- ============================================================
-- action_log
-- ============================================================

CREATE TABLE action_log (
                            id BIGSERIAL PRIMARY KEY,

                            interaction_id BIGINT NOT NULL,

                            action VARCHAR(100) NOT NULL,

                            result VARCHAR(50),

                            detail JSONB,

                            created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                            CONSTRAINT fk_action_log_interaction
                                FOREIGN KEY (interaction_id)
                                    REFERENCES interaction(id)
                                    ON DELETE CASCADE
);