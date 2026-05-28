-- V8__create_supporting.sql
-- Creates notifications, audit_logs, and refresh_tokens tables.
-- audit_logs is append-only — never UPDATE or DELETE rows.
-- refresh_tokens: revoked=true means the token cannot be used; expired tokens cleaned by scheduled job.

CREATE TABLE notifications (
    id         UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID    NOT NULL,
    title      VARCHAR(255) NOT NULL,
    message    TEXT    NOT NULL,
    is_read    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE audit_logs (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id      UUID         NOT NULL,
    action        VARCHAR(64)  NOT NULL,
    resource_type VARCHAR(64),
    resource_id   VARCHAR(64),
    ip_address    VARCHAR(45),
    user_agent    TEXT,
    metadata      JSONB,
    created_at    TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT fk_audit_actor FOREIGN KEY (actor_id) REFERENCES users(id)
);

CREATE TABLE refresh_tokens (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL,
    token_hash  VARCHAR(64)  NOT NULL UNIQUE,
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT fk_rt_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_notifications_user_id     ON notifications(user_id);
CREATE INDEX idx_notifications_is_read     ON notifications(user_id, is_read);
CREATE INDEX idx_audit_logs_actor_id       ON audit_logs(actor_id);
CREATE INDEX idx_audit_logs_action         ON audit_logs(action);
CREATE INDEX idx_audit_logs_created_at     ON audit_logs(created_at);
CREATE INDEX idx_audit_logs_resource       ON audit_logs(resource_type, resource_id);
CREATE INDEX idx_refresh_tokens_hash       ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_tokens_user_id    ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);
