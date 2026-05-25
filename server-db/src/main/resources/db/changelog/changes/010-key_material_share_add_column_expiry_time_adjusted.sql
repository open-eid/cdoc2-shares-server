-- liquibase formatted sql
-- changeset cdoc2_capsule_add_column_expiry_time_adjusted:6
ALTER TABLE key_material_share
ADD expiry_time_adjusted BOOLEAN DEFAULT FALSE;
