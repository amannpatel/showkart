CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS citext;

CREATE TABLE users (
    user_id       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email         CITEXT UNIQUE NOT NULL,
    password_hash VARCHAR(72)   NOT NULL,
    roles         VARCHAR(255)  NOT NULL DEFAULT 'ROLE_USER',
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);
