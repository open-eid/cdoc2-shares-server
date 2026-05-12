-- liquibase formatted sql
-- changeset expired_session_nonce_cleanup_func:8 runOnChange:true
CREATE OR REPLACE FUNCTION expired_session_nonce_cleanup()
    RETURNS INTEGER
AS '
    DECLARE deleted INTEGER := 0;
BEGIN
	DELETE FROM session_nonce
	WHERE id IN
	(SELECT id FROM session_nonce
		WHERE created_at < (CURRENT_TIMESTAMP - interval ''24 hours'')
		LIMIT 1000
	);

	GET DIAGNOSTICS deleted = ROW_COUNT;
	RETURN deleted;
END
'
LANGUAGE plpgsql;