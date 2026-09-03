CREATE TABLE refresh_token (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID         NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    token_hash   VARCHAR(64)  NOT NULL UNIQUE,
    issued_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    expires_at   TIMESTAMPTZ  NOT NULL,
    revoked_at   TIMESTAMPTZ  NULL,
    replaced_by  UUID         NULL REFERENCES refresh_token(id) ON DELETE SET NULL
);

CREATE INDEX idx_refresh_token_user_active
    ON refresh_token (user_id)
    WHERE revoked_at IS NULL;
