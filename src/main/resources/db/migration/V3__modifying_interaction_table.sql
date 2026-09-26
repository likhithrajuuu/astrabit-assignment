ALTER TABLE interaction
ALTER COLUMN severity TYPE VARCHAR(50)
USING severity::varchar;