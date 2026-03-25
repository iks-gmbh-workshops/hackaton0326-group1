CREATE TABLE group_activity (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL REFERENCES app_group (id),
    description VARCHAR(255) NOT NULL,
    details TEXT,
    location VARCHAR(255) NOT NULL,
    scheduled_at TIMESTAMPTZ NOT NULL,
    created_by_user_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_group_activity_group_id
    ON group_activity (group_id);

CREATE INDEX idx_group_activity_scheduled_at
    ON group_activity (scheduled_at);

CREATE TABLE activity_participant (
    id BIGSERIAL PRIMARY KEY,
    activity_id BIGINT NOT NULL REFERENCES group_activity (id),
    group_membership_id BIGINT NOT NULL REFERENCES group_membership (id),
    response_status VARCHAR(32) NOT NULL,
    response_note TEXT,
    responded_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    removed_at TIMESTAMPTZ,
    assignment_slot BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_activity_participant_activity_id
    ON activity_participant (activity_id);

CREATE INDEX idx_activity_participant_group_membership_id
    ON activity_participant (group_membership_id);

CREATE UNIQUE INDEX uq_activity_participant_active_assignment
    ON activity_participant (activity_id, group_membership_id, assignment_slot);
