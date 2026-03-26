ALTER TABLE group_membership
    ADD COLUMN open_user_relation BOOLEAN;

ALTER TABLE group_membership
    ADD COLUMN open_email_relation BOOLEAN;

UPDATE group_membership
SET open_user_relation = CASE
        WHEN user_id IS NOT NULL AND status IN ('ACTIVE', 'INVITED') THEN TRUE
        ELSE NULL
    END,
    open_email_relation = CASE
        WHEN normalized_invite_email IS NOT NULL AND status IN ('ACTIVE', 'INVITED') THEN TRUE
        ELSE NULL
    END;

DROP INDEX uq_group_membership_active_user;

DROP INDEX uq_group_membership_active_email;

CREATE UNIQUE INDEX uq_group_membership_active_user
    ON group_membership (group_id, user_id, open_user_relation);

CREATE UNIQUE INDEX uq_group_membership_active_email
    ON group_membership (group_id, normalized_invite_email, open_email_relation);
