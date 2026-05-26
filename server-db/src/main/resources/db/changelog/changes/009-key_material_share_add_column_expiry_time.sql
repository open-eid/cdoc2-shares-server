-- liquibase formatted sql
-- changeset cdoc2_capsule_add_column_expiry_time:2
ALTER TABLE key_material_share
ADD expiry_time timestamp;
