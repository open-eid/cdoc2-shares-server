package ee.cyber.cdoc2.server;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Types;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ee.cyber.cdoc2.server.exeptions.JobFailureException;
import ee.cyber.cdoc2.server.model.entity.KeyShareNonceDb;
import ee.cyber.cdoc2.server.model.entity.SessionNonceDb;


/**
 * Cleanup job for expired CDOC2 key share nonces from {@link KeyShareNonceDb}
 * and expired session nonces from {@link SessionNonceDb}
 */
@Component
@Slf4j
@RequiredArgsConstructor
public final class ExpiredNonceCleanUpJob {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Executes the stored function {@code expired_key_material_share_nonce_cleanup()} in CDOC2 database
     */
    @Scheduled(cron = "${key-share-nonce.expired.clean-up.cron}")
    public int cleanUpExpiredShareNonce() {
        return executeCleanup(
            "expired_key_material_share_nonce_cleanup",
            "key share nonce's"
        );
    }

    /**
     * Executes the stored function {@code expired_session_nonce_cleanup()} in CDOC2 database
     */
    @Scheduled(cron = "${session-nonce.expired.clean-up.cron}")
    public int cleanUpExpiredSessionNonce() {
        return executeCleanup(
            "expired_session_nonce_cleanup",
            "session nonce's"
        );
    }

    private int executeCleanup(String storedFunction, String nonceType) {
        log.debug("Executing expired {} deletion from database", nonceType);

        try {
            Integer deleted = jdbcTemplate.execute((Connection connection) -> {
                String query = "{? = call " + storedFunction + "()}";
                try (CallableStatement stmt = connection.prepareCall(query)) {
                    stmt.registerOutParameter(1, Types.INTEGER);
                    stmt.execute();
                    return stmt.getInt(1);
                }
            });
            if (deleted == null || deleted == 0) {
                log.debug("No expired {}", nonceType);
                return 0;
            } else {
                log.info("Total number of successfully deleted expired {} is {}", nonceType, deleted);
                return deleted;
            }
        } catch (Exception e) {
            String errorMsg = "Expired " + nonceType + " deletion has failed";
            log.error(errorMsg);
            throw new JobFailureException(errorMsg, e);
        }
    }
}
