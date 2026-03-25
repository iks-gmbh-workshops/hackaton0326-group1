CREATE TABLE group_membership (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL REFERENCES app_group (id),
    user_id VARCHAR(64),
    invite_email VARCHAR(320),
    normalized_invite_email VARCHAR(320),
    display_name VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    is_admin BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    joined_at TIMESTAMPTZ,
    left_at TIMESTAMPTZ,
    removed_at TIMESTAMPTZ
);

CREATE INDEX idx_group_membership_group_id
    ON group_membership (group_id);

CREATE INDEX idx_group_membership_user_id
    ON group_membership (user_id);

CREATE INDEX idx_group_membership_invite_email
    ON group_membership (normalized_invite_email);

CREATE UNIQUE INDEX uq_group_membership_active_user
    ON group_membership (group_id, user_id, status);

CREATE UNIQUE INDEX uq_group_membership_active_email
    ON group_membership (group_id, normalized_invite_email, status);
