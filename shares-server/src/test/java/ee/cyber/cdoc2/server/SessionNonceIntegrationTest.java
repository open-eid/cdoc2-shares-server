package ee.cyber.cdoc2.server;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ee.cyber.cdoc2.server.api.SessionNonceApiService;
import ee.cyber.cdoc2.server.model.entity.SessionNonceDb;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SessionNonceIntegrationTest extends BaseInitializationTest {

    // Matches the {@code LIMIT 1000} in expired_session_nonce_cleanup() (changeset 008).
    private static final int CLEANUP_BATCH_LIMIT = 1000;

    @Autowired
    private ExpiredNonceCleanUpJob cleanUpJob;

    @Autowired
    private SessionNonceApiService sessionNonceApiService;

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

    @Test
    void shouldDeleteOnlyBatchLimitOfExpiredSessionNoncesPerRun() {
        int total = CLEANUP_BATCH_LIMIT + 1;
        persistExpiredSessionNonces(total);
        assertEquals(total, this.sessionNonceRepository.count());

        int deletedFirstRun = cleanUpJob.cleanUpExpiredSessionNonce();
        assertEquals(CLEANUP_BATCH_LIMIT, deletedFirstRun);
        assertEquals(total - CLEANUP_BATCH_LIMIT, this.sessionNonceRepository.count());

        int deletedSecondRun = cleanUpJob.cleanUpExpiredSessionNonce();
        assertEquals(total - CLEANUP_BATCH_LIMIT, deletedSecondRun);
        assertEquals(0, this.sessionNonceRepository.count());
    }

    @Test
    void shouldGenerateUniqueSessionNoncesOnConsecutiveCalls() {
        var first = sessionNonceApiService.generateSessionNonce(null).getBody();
        var second = sessionNonceApiService.generateSessionNonce(null).getBody();

        assertNotNull(first);
        assertNotNull(second);
        assertNotEquals(first.getNonce(), second.getNonce());
        assertEquals(2, this.sessionNonceRepository.count());
    }

    private SessionNonceDb createSessionNonce() {
        SessionNonceDb nonce = new SessionNonceDb();
        nonce.setNonce("123".getBytes());
        return this.sessionNonceRepository.save(nonce);
    }

    private void persistExpiredSessionNonces(int count) {
        List<SessionNonceDb> nonces = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            SessionNonceDb nonce = new SessionNonceDb();
            nonce.setNonce(("nonce-" + i).getBytes());
            nonces.add(nonce);
        }
        this.sessionNonceRepository.saveAll(nonces);

        Instant expired = Instant.now().minus(25, ChronoUnit.HOURS);
        nonces.forEach(n -> n.setCreatedAt(expired));
        List<SessionNonceDb> saved = this.sessionNonceRepository.saveAll(nonces);

        Instant oneDayAgo = Instant.now().minus(1, ChronoUnit.DAYS);
        assertTrue(saved.stream().allMatch(n -> n.getCreatedAt().isBefore(oneDayAgo)));
    }
}
