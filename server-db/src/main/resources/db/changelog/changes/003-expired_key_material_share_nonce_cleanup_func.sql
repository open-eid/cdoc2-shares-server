-- liquibase formatted sql
-- changeset expired_key_material_share_nonce_cleanup_func:3 runOnChange:true
CREATE OR REPLACE FUNCTION expired_key_material_share_nonce_cleanup()
    RETURNS INTEGER
AS '
    DECLARE deleted INTEGER := 0;
BEGIN
	DELETE FROM key_material_share_nonce
	WHERE id IN
	(SELECT id FROM key_material_share_nonce
		WHERE created_at < (CURRENT_TIMESTAMP - interval ''24 hours'')
		LIMIT 1000
	);

	GET DIAGNOSTICS deleted = ROW_COUNT;
	RETURN deleted;
END
'
LANGUAGE plpgsql;
