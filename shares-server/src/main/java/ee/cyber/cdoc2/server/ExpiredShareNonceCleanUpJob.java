package ee.cyber.cdoc2.server;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Types;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ee.cyber.cdoc2.server.config.DbConnectionConfigProperties;
import ee.cyber.cdoc2.server.exeptions.JobFailureException;
import ee.cyber.cdoc2.server.model.entity.KeyShareNonceDb;


/**
 * Cleanup job for expired CDOC2 key share nonces from {@link KeyShareNonceDb}
 */
@Component
@Slf4j
@RequiredArgsConstructor
public final class ExpiredShareNonceCleanUpJob {

    private Connection dbConnection;
    private final DbConnectionConfigProperties configProperties;

    @PostConstruct
    void init() {
        dbConnection = createDbConnection();
    }

    /**
     * Executes the stored function {@code expired_key_material_share_nonce_cleanup()} in CDOC2 database
     */
    @Scheduled(cron = "${key-share-nonce.expired.clean-up.cron}")
    public int cleanUpExpiredShareNonce() {
        log.debug("Executing expired key share nonces deletion from database");

        String query = "{? = call expired_key_material_share_nonce_cleanup()}";
        try (CallableStatement stmt = dbConnection.prepareCall(query)) {
            stmt.registerOutParameter(1, Types.INTEGER);
            stmt.execute();
            return getExecutionResult(stmt);
        } catch (SQLException e) {
            String errorMsg = "Expired key share nonces deletion has failed";
            log.error(errorMsg);
            throw new JobFailureException(errorMsg, e);
        }
    }

    int getExecutionResult(CallableStatement stmt) throws SQLException {
        int deleted = stmt.getInt(1);
        if (deleted == 0) {
            log.debug("No expired key share nonces found to delete");
        } else {
            log.info("Total number of successfully deleted expired key share nonces is {}",
                deleted);
        }
        return deleted;
    }

    private Connection createDbConnection() {
        try {
            return DriverManager.getConnection(
                configProperties.url(),
                configProperties.username(),
                configProperties.password()
            );
        } catch (SQLException e) {
            String errorMsg = "Failed to establish database connection";
            log.error(errorMsg);
            throw new JobFailureException(errorMsg, e);
        }
    }

    @PreDestroy
    public void preDestroy() throws SQLException {
        dbConnection.close();
    }

}
