CREATE TABLE terms_version (
    id BIGSERIAL PRIMARY KEY,
    version VARCHAR(64) NOT NULL UNIQUE,
    content_slug VARCHAR(128) NOT NULL UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_terms_version_is_active
    ON terms_version (is_active);

CREATE TABLE terms_consent (
    id BIGSERIAL PRIMARY KEY,
    keycloak_user_id VARCHAR(64) NOT NULL,
    terms_version_id BIGINT NOT NULL REFERENCES terms_version (id),
    consent_type VARCHAR(32) NOT NULL,
    consented_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_terms_consent_user_consented_at
    ON terms_consent (keycloak_user_id, consented_at DESC);

INSERT INTO terms_version (version, content_slug, is_active)
VALUES ('2026-03', 'drumdibum-agb-2026-03', TRUE);
