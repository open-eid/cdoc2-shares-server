-- liquibase formatted sql
-- changeset key_material_share_nonce_create_index:4
ALTER TABLE key_material_share_nonce
ADD CONSTRAINT unique_share_id UNIQUE (share_id);

CREATE INDEX idx_share_id_and_nonce ON key_material_share_nonce (share_id, nonce);
