-- liquibase formatted sql
-- changeset session_nonce_create_index:6
CREATE INDEX idx_session_nonce_nonce ON session_nonce (nonce);
