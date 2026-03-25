CREATE TABLE group_invitation (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL REFERENCES app_group (id),
    membership_id BIGINT REFERENCES group_membership (id),
    token_id BIGINT REFERENCES group_invitation_token (id),
    invited_by_user_id VARCHAR(64) NOT NULL,
    channel VARCHAR(32) NOT NULL,
    mail_type VARCHAR(32) NOT NULL,
    target_label VARCHAR(320) NOT NULL,
    target_email VARCHAR(320),
    normalized_target_email VARCHAR(320),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    claimed_at TIMESTAMPTZ,
    claimed_by_user_id VARCHAR(64)
);

CREATE INDEX idx_group_invitation_group_id
    ON group_invitation (group_id);

CREATE INDEX idx_group_invitation_membership_id
    ON group_invitation (membership_id);

CREATE INDEX idx_group_invitation_token_id
    ON group_invitation (token_id);
