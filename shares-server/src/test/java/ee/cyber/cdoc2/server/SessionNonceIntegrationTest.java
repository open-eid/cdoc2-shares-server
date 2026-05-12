package ee.cyber.cdoc2.server;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ee.cyber.cdoc2.server.model.entity.SessionNonceDb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SessionNonceIntegrationTest extends BaseInitializationTest {

    @Autowired
    private ExpiredNonceCleanUpJob cleanUpJob;

    @BeforeEach
    public void prepareDatabaseState() {
        this.shareNonceRepository.deleteAll();
        this.shareRepository.deleteAll();
        this.sessionNonceRepository.deleteAll();
    }

    @Test
    void shouldCleanUpExpiredSessionNonce() {
        SessionNonceDb nonce = createSessionNonce();

        Instant desiredCreationTime = Instant.now().minus(1, ChronoUnit.DAYS);
        nonce.setCreatedAt(desiredCreationTime);
        SessionNonceDb updatedNonce = this.sessionNonceRepository.save(nonce);

        long countExisting = this.sessionNonceRepository.count();
        assertEquals(1, countExisting);

        Instant creationTime = updatedNonce.getCreatedAt();
        Instant now = Instant.now();
        Instant oneDayAgo = now.minus(1, ChronoUnit.DAYS);
        boolean isExpiredByMoreThanOneDay = now.isAfter(creationTime) && creationTime.isBefore(oneDayAgo);
        assertTrue(isExpiredByMoreThanOneDay);

        int deletedSessionNonce = cleanUpJob.cleanUpExpiredSessionNonce();
        assertEquals(1, deletedSessionNonce);

        long countRemaining = this.sessionNonceRepository.count();
        assertEquals(0, countRemaining);
    }

    @Test
    void shouldNotDeleteSessionNonceCreatedLessThan24HoursAgo() {
        SessionNonceDb nonce = createSessionNonce();

        // 1439 minutes = 24 hours - 1 minute
        Instant desiredCreationTime = Instant.now().minus(1439, ChronoUnit.MINUTES);
        nonce.setCreatedAt(desiredCreationTime);
        SessionNonceDb updatedNonce = this.sessionNonceRepository.save(nonce);

        int deletedSessionNonce = cleanUpJob.cleanUpExpiredSessionNonce();
        assertEquals(0, deletedSessionNonce);

        Instant creationTime = updatedNonce.getCreatedAt();
        Instant now = Instant.now();
        Instant lessThanOneDayAgo = now.minus(1, ChronoUnit.DAYS);
        boolean isNotExpired = now.isAfter(creationTime) && creationTime.isAfter(lessThanOneDayAgo);
        assertTrue(isNotExpired);
    }

    private SessionNonceDb createSessionNonce() {
        SessionNonceDb nonce = new SessionNonceDb();
        nonce.setNonce("123".getBytes());
        return this.sessionNonceRepository.save(nonce);
    }
}
