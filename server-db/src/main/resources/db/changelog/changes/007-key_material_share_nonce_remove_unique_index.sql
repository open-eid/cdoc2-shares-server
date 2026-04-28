-- liquibase formatted sql
-- changeset key_material_share_nonce_drop_unique_constraint:5
ALTER TABLE key_material_share_nonce
DROP CONSTRAINT unique_share_id;