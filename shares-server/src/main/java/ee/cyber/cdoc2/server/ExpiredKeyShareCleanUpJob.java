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


/**
 * Cleanup job for expired key_material_share table records
 */
@Component
@Slf4j
@RequiredArgsConstructor
public final class ExpiredKeyShareCleanUpJob {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Executes the stored function {@code expired_key_material_share_cleanup()} in database
     */
    @Scheduled(cron = "${key-share.expired.clean-up.cron}")
    public int cleanUpExpiredKeyShares() {
        log.debug("Executing expired key share deletion from database");

        try {
            Integer deleted = jdbcTemplate.execute((Connection connection) -> {
                String query = "{? = call expired_key_material_share_cleanup()}";
                try (CallableStatement stmt = connection.prepareCall(query)) {
                    stmt.registerOutParameter(1, Types.INTEGER);
                    stmt.execute();
                    return stmt.getInt(1);
                }
            });

            if (deleted == null || deleted == 0) {
                log.debug("No expired key shares");
                return 0;
            } else {
                log.info("Total number of successfully deleted expired key shares is {}", deleted);
                return deleted;
            }
        } catch (Exception e) {
            String errorMsg = "Expired key share deletion has failed";
            log.error(errorMsg, e);
            throw new JobFailureException(errorMsg, e);
        }
    }
}
