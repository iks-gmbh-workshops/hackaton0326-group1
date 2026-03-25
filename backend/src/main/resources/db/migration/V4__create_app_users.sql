CREATE TABLE app_user (
    keycloak_user_id VARCHAR(64) PRIMARY KEY,
    nickname VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(320) NOT NULL UNIQUE,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    status VARCHAR(32) NOT NULL,
    enabled BOOLEAN NOT NULL,
    email_verified BOOLEAN NOT NULL,
    keycloak_role VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    verified_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_app_user_status
    ON app_user (status);
