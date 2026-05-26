-- liquibase formatted sql
-- changeset expired_key_material_share_cleanup_func:5 runOnChange:true
CREATE OR REPLACE FUNCTION expired_key_material_share_cleanup()
    RETURNS INTEGER
AS '
    DECLARE deleted INTEGER := 0;
BEGIN
	DELETE FROM key_material_share WHERE share_id IN
		(SELECT share_id FROM key_material_share WHERE expiry_time < CURRENT_TIMESTAMP LIMIT 1000);
	GET DIAGNOSTICS deleted = ROW_COUNT;
	RETURN deleted;
END
'
LANGUAGE plpgsql;
