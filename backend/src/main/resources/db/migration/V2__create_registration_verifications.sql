CREATE TABLE registration_verification (
    id BIGSERIAL PRIMARY KEY,
    keycloak_user_id VARCHAR(64) NOT NULL,
    nickname VARCHAR(255) NOT NULL,
    email VARCHAR(320) NOT NULL,
    token_hash CHAR(64) NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    verified_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_registration_verification_status_expires_at
    ON registration_verification (status, expires_at);
