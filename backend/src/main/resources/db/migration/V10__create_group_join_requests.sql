CREATE TABLE group_join_request (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL REFERENCES app_group (id),
    requested_by_user_id VARCHAR(64) NOT NULL,
    requested_by_display_name VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    comment TEXT,
    review_comment TEXT,
    reviewed_by_user_id VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMPTZ
);

CREATE INDEX idx_group_join_request_group_id
    ON group_join_request (group_id);

CREATE INDEX idx_group_join_request_requested_by_user_id
    ON group_join_request (requested_by_user_id);

CREATE UNIQUE INDEX uq_group_join_request_pending
    ON group_join_request (group_id, requested_by_user_id, status);
