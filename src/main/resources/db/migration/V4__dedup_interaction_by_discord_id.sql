-- ============================================================
-- Dedup on Discord's own interaction id.
--
-- ============================================================

ALTER TABLE interaction
    ADD COLUMN discord_interaction_id VARCHAR(32);

-- Backfill from the raw payload already stored for every existing row.
UPDATE interaction
SET discord_interaction_id = payload ->> 'id'
WHERE discord_interaction_id IS NULL;

ALTER TABLE interaction
    ALTER COLUMN discord_interaction_id SET NOT NULL;

ALTER TABLE interaction
    ADD CONSTRAINT uq_interaction_discord_id UNIQUE (discord_interaction_id);
