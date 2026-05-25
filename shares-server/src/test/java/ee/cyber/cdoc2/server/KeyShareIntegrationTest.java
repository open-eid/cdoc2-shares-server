package ee.cyber.cdoc2.server;

import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ee.cyber.cdoc2.server.model.entity.KeyShareDb;
import ee.cyber.cdoc2.server.model.entity.KeyShareNonceDb;

import static org.junit.jupiter.api.Assertions.*;


@Slf4j
public abstract class KeyShareIntegrationTest extends BaseInitializationTest {

    @Autowired
    private ExpiredNonceCleanUpJob nonceCleanUpJob;

    @Autowired
    private ExpiredKeyShareCleanUpJob keyShareCleanUpJob;

    @BeforeEach
    public void prepareDatabaseState() {
        this.shareNonceRepository.deleteAll();
        this.shareRepository.deleteAll();
        this.sessionNonceRepository.deleteAll();
    }

    @Test
    void testKeyShareJpaConstraints() {
        KeyShareDb model = new KeyShareDb();

        byte[] bytes = new byte[128];
        model.setShare(bytes);
        model.setRecipient(null);

        Throwable cause = assertThrows(Throwable.class, () -> this.shareRepository.save(model));
        assertThrowsConstraintViolationException(cause);
    }

    @Test
    void testJpaSaveAndFindByIdForKeyShare() {
        // test that jpa is up and running (expect no exceptions)
        this.shareRepository.count();

        KeyShareDb keyShare = createKeyShare();

        assertNotNull(keyShare);
        String shareId = keyShare.getShareId();
        assertNotNull(shareId);
        log.debug("Created {}", shareId);

        Optional<KeyShareDb> retrievedOpt = this.shareRepository.findById(shareId);
        assertTrue(retrievedOpt.isPresent());

        var dbRecord = retrievedOpt.get();
        assertNotNull(dbRecord.getShareId());
        assertNotNull(dbRecord.getCreatedAt());
        log.debug("Retrieved {}", dbRecord);
    }

    @Test
    void testJpaSaveForKeyShareNonce() {
        // test that jpa is up and running (expect no exceptions)
        this.shareNonceRepository.count();

        KeyShareDb keyShare = createKeyShare();

        assertNotNull(keyShare);
        String shareId = keyShare.getShareId();
        assertNotNull(shareId);
        log.debug("Created {}", shareId);

        KeyShareNonceDb keyShareNonce = new KeyShareNonceDb();
        keyShareNonce.setShareId(shareId);
        KeyShareNonceDb dbNonceRecord = this.shareNonceRepository.save(keyShareNonce);

        assertEquals(shareId, dbNonceRecord.getShareId());
        assertNotNull(dbNonceRecord.getNonce());
        log.debug("Retrieved {}", dbNonceRecord);

        // check findByShareIdAndNonce
        Optional<KeyShareNonceDb> retrievedOpt = this.shareNonceRepository.findByShareIdAndNonce(shareId,
            dbNonceRecord.getNonce());

        assertTrue(retrievedOpt.isPresent());
        String nonceHex = HexFormat.of().formatHex(dbNonceRecord.getNonce());
        KeyShareNonceDb retrievedNonce = retrievedOpt.get();
        log.debug("findByShareIdAndNonce({},{}): {}", shareId, nonceHex, retrievedNonce);
        assertEquals(shareId, retrievedNonce.getShareId());
        assertArrayEquals(dbNonceRecord.getNonce(), retrievedNonce.getNonce());
    }

    @Test
    void shouldCleanUpExpiredKeyShareNonce() {
        KeyShareDb keyShare = createKeyShare();

        KeyShareNonceDb nonce = createKeyShareNonce(keyShare.getShareId());

        Instant desiredCreationTime = Instant.now().minus(1, ChronoUnit.DAYS);
        nonce.setCreatedAt(desiredCreationTime);
        KeyShareNonceDb updatedNonce = this.shareNonceRepository.save(nonce);

        long countExisting = this.shareNonceRepository.count();
        assertEquals(1, countExisting);

        Instant creationTime = updatedNonce.getCreatedAt();
        Instant now = Instant.now();
        Instant oneDayAgo = now.minus(1, ChronoUnit.DAYS);
        boolean isExpiredByMoreThanOneDay = now.isAfter(creationTime) && creationTime.isBefore(oneDayAgo);
        assertTrue(isExpiredByMoreThanOneDay);

        int deletedShareNonce = nonceCleanUpJob.cleanUpExpiredShareNonce();
        assertEquals(1, deletedShareNonce);

        long countRemaining = this.shareNonceRepository.count();
        assertEquals(0, countRemaining);
    }

    @Test
    void shouldCleanUpExpiredKeyShares() {
        KeyShareDb keyShare = defaultKeyShare();
        KeyShareDb keyShareExpired = defaultKeyShare();
        Instant expiryTime = Instant.now().minusSeconds(60);

        keyShareExpired.setExpiryTime(expiryTime);

        createKeyShare(keyShare);
        createKeyShare(keyShareExpired);

        List<KeyShareDb> keySharesBeforeCleanup = shareRepository.findAll();
        assertEquals(2, keySharesBeforeCleanup.size());
        assertTrue(keySharesBeforeCleanup.stream()
            .anyMatch(k -> k.getShareId().equals(keyShare.getShareId()))
        );

        assertTrue(keySharesBeforeCleanup.stream()
            .anyMatch(k -> k.getShareId().equals(keyShareExpired.getShareId()))
        );

        keyShareCleanUpJob.cleanUpExpiredKeyShares();

        List<KeyShareDb> keySharesAfterCleanup = shareRepository.findAll();
        assertEquals(1, keySharesAfterCleanup.size());

        assertEquals(
            keySharesAfterCleanup.get(0).getShareId(),
            keyShare.getShareId()
        );
    }

    @Test
    void shouldNotDeleteKeyShareNonceCreatedLessThan24HoursAgo() {
        KeyShareDb keyShare = createKeyShare();

        KeyShareNonceDb nonce = createKeyShareNonce(keyShare.getShareId());

        // 1439 minutes = 24 hours - 1 minute
        Instant desiredCreationTime = Instant.now().minus(1439, ChronoUnit.MINUTES);
        nonce.setCreatedAt(desiredCreationTime);
        KeyShareNonceDb updatedNonce = this.shareNonceRepository.save(nonce);

        int deletedShareNonce = nonceCleanUpJob.cleanUpExpiredShareNonce();
        assertEquals(0, deletedShareNonce);

        Instant creationTime = updatedNonce.getCreatedAt();
        Instant now = Instant.now();
        Instant lessThanOneDayAgo = now.minus(1, ChronoUnit.DAYS);
        boolean isNotExpired = now.isAfter(creationTime) && creationTime.isAfter(lessThanOneDayAgo);
        assertTrue(isNotExpired);
    }

    private KeyShareDb defaultKeyShare() {
        KeyShareDb keyShareDb = new KeyShareDb();
        keyShareDb.setRecipient("1234567891011_12_is_min_length");
        byte[] bytes = new byte[128];
        keyShareDb.setShare(bytes);
        keyShareDb.setExpiryTime(EXPIRY_TIME);
        keyShareDb.setExpiryTimeAdjusted(EXPIRY_TIME_ADJUSTED);
        return createKeyShare(keyShareDb);
    }

    private KeyShareDb createKeyShare() {
        return this.shareRepository.save(defaultKeyShare());
    }

    private KeyShareDb createKeyShare(KeyShareDb keyShare) {
        return this.shareRepository.save(keyShare);
    }

    private KeyShareNonceDb createKeyShareNonce(String shareId) {
        KeyShareNonceDb nonce = new KeyShareNonceDb();
        nonce.setShareId(shareId);
        nonce.setNonce("123".getBytes());
        return this.shareNonceRepository.save(nonce);
    }

}
