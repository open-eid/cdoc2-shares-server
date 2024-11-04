-- liquibase formatted sql
-- changeset key_material_share:1
CREATE TABLE key_material_share (
    share_id VARCHAR(34) NOT NULL,
    recipient VARCHAR(32) NOT NULL,
    share bytea NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_share_id PRIMARY KEY (share_id),
    UNIQUE(share_id)
);
