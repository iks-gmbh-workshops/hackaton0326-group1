CREATE TABLE app_metadata (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    label VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO app_metadata (code, label)
VALUES ('scaffold', 'HeuermannPlus bootstrap complete');
