package ee.cyber.cdoc2.server;

import lombok.extern.slf4j.Slf4j;

import java.util.HexFormat;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import ee.cyber.cdoc2.server.model.entity.KeyShareDb;
import ee.cyber.cdoc2.server.model.entity.KeyShareNonceDb;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


@Slf4j
abstract class KeyShareIntegrationTest extends BaseInitializationTest {

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

        KeyShareDb model = new KeyShareDb();
        model.setRecipient("1234567891011_12_is_min_length");
        byte[] bytes = new byte[128];
        model.setShare(bytes);
        KeyShareDb saved = this.shareRepository.save(model);

        assertNotNull(saved);
        String shareId = saved.getShareId();
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

        KeyShareDb keyShareDb = new KeyShareDb();
        keyShareDb.setRecipient("1234567891011_12_is_min_length");
        byte[] bytes = new byte[128];
        keyShareDb.setShare(bytes);
        KeyShareDb keyShare = this.shareRepository.save(keyShareDb);

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
        KeyShareNonceDb retrievedNonce =  retrievedOpt.get();
        log.debug("findByShareIdAndNonce({},{}): {}", shareId, nonceHex, retrievedNonce);
        assertEquals(shareId, retrievedNonce.getShareId());
        assertArrayEquals(dbNonceRecord.getNonce(), retrievedNonce.getNonce());
    }

}
