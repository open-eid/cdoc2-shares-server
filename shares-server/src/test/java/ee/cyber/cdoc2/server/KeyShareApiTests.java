package ee.cyber.cdoc2.server;

import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import ee.cyber.cdoc2.client.Cdoc2KeySharesApiClient;
import ee.cyber.cdoc2.client.api.ApiException;
import ee.cyber.cdoc2.client.model.KeyShare;
import ee.cyber.cdoc2.client.model.NonceResponse;
import ee.cyber.cdoc2.server.config.MonitoringConfigProperties;
import ee.cyber.cdoc2.server.model.entity.KeyShareDb;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


@Slf4j
class KeyShareApiTests extends KeyShareIntegrationTest {

    private static final byte[] SHARE = new byte[128];
    private static final String SHARE_RECIPIENT = "Recipient_for_key_share";

    @Autowired
    private MonitoringConfigProperties configProperties;

    private Cdoc2KeySharesApiClient client;

    @BeforeEach
    public void setup() throws Exception {
        client = createClient();
    }

    @Test
    void shouldGetKeyShare() throws Exception {
        KeyShare keyShare = createKeyShare();
        keyShare.setRecipient(TestData.TEST_ETSI_RECIPIENT);

        String shareId = this.saveKeyShare(keyShare).getShareId();
        String nonce = client.createNonce(shareId).getNonce();
        String xAuthTicket = TestData.generateTestAuthTicket(TestData.TEST_IDENTIFIER, baseUrl, shareId, nonce);

        Optional<KeyShare> response = client.getKeyShare(shareId, xAuthTicket, TestData.TEST_CERT_PEM);


        assertTrue(response.isPresent());
        KeyShare savedKeyShare = response.get();
        assertEquals(keyShare.getRecipient(), savedKeyShare.getRecipient());
        assertArrayEquals(keyShare.getShare(), savedKeyShare.getShare());
    }

    @Test
    void shouldFailToGetKeyShareWithBadRequest() {
        String shareId = "short";
        String xAuthTicket = "";
        String xAuthCert = "";

        ApiException ex = assertThrows(
            ApiException.class,
            () -> client.getKeyShare(shareId, xAuthTicket, xAuthCert)
        );

        assertBadRequest(ex.getCode());
    }

    @Test
    void shouldFailToGetKeyShareWithNotFound() throws ApiException {
        String shareId = "SHARE_ID_MIN_LENGTH_SHOULD_BE_32";

        String nonce = "random";
        String xAuthTicket = TestData.generateTestAuthTicket(TestData.TEST_IDENTIFIER, baseUrl, shareId, nonce);

        Optional<KeyShare> keyShare = client.getKeyShare(shareId, xAuthTicket, TestData.TEST_CERT_PEM);

        assertTrue(keyShare.isEmpty());
    }

    @Test
    void shouldCreateKeyShare() throws Exception {
        var keyShare = createKeyShare();

        String response = client.createKeyShare(keyShare);

        assertKeyShare(keyShare, response);
    }

    @Test
    void shouldFailToCreateKeyShareWithShortShare() {
        byte[] shortShare = new byte[10];
        var keyShare = new ee.cyber.cdoc2.client.model.KeyShare()
            .recipient(SHARE_RECIPIENT)
            .share(shortShare);

        assertThrowsBadRequest(keyShare);
    }

    @Test
    void shouldFailToCreateKeyShareWithTooLongShare() {
        byte[] tooLongShare = new byte[254];
        var keyShare = new ee.cyber.cdoc2.client.model.KeyShare()
            .recipient(SHARE_RECIPIENT)
            .share(tooLongShare);

        assertThrowsBadRequest(keyShare);
    }

    @Test
    void shouldFailToCreateKeyShareWithShortRecipient() {
        var keyShare = new ee.cyber.cdoc2.client.model.KeyShare()
            .recipient("short")
            .share(SHARE);

        assertThrowsBadRequest(keyShare);
    }

    @Test
    void shouldFailToCreateKeyShareWithTooLongRecipient() {
        var keyShare = new ee.cyber.cdoc2.client.model.KeyShare()
            .recipient("Tooooooooooooooooooooooooooo_looooooooooooooooooooooooong_recipient")
            .share(SHARE);

        assertThrowsBadRequest(keyShare);
    }

    @Test
    void shouldCreateKeyShareNonce() throws Exception {
        var keyShare = createKeyShare();

        String shareId = this.saveKeyShare(keyShare).getShareId();

        NonceResponse response = client.createNonce(shareId);
        assertNotNull(response);
    }

    @Test
    void shouldFailToCreateKeyShareNonceWithNotFoundKeyShareId() {
        String shareId = "SHARE_ID_MIN_LENGTH_SHOULD_BE_32";

        ApiException ex = assertThrows(
            ApiException.class,
            () -> client.createNonce(shareId)
        );

        assertEquals(HttpStatus.NOT_FOUND.value(), ex.getCode());
    }

    private Cdoc2KeySharesApiClient createClient() throws Exception {
        var builder = Cdoc2KeySharesApiClient.builder();
        builder.withBaseUrl(baseUrl);
        builder.withUsername(configProperties.username());
        builder.withPassword(configProperties.password());
        builder.withTrustKeyStore(CLIENT_TRUST_STORE);
        builder.withDebuggingEnabled(true);

        return builder.build();
    }

    private void assertKeyShare(KeyShare keyShare, String shareId) {
        assertNotNull(shareId);

        this.assertShareExistsInDb(shareId, keyShare);
    }

    private void assertThrowsBadRequest(KeyShare keyShare) {
        ApiException ex = assertThrows(
            ApiException.class,
            () -> client.createKeyShare(keyShare)
        );
        assertBadRequest(ex.getCode());
    }

    private void assertBadRequest(int responseCode) {
        assertEquals(HttpStatus.BAD_REQUEST.value(), responseCode);
    }

    private void assertShareExistsInDb(String shareId, KeyShare keyShare) {
        var keyShareDb = this.shareRepository.findById(shareId);
        assertTrue(keyShareDb.isPresent());
        var dbCapsule = keyShareDb.get();

        assertEquals(keyShare.getRecipient(), dbCapsule.getRecipient());
        assertArrayEquals(keyShare.getShare(), dbCapsule.getShare());
    }

    private ee.cyber.cdoc2.client.model.KeyShare createKeyShare() {
        return new ee.cyber.cdoc2.client.model.KeyShare()
            .recipient(SHARE_RECIPIENT)
            .share(SHARE);
    }

    /**
     * Saves the key share into database
     * @param dto the key share dto
     * @return the saved key share
     */
    protected KeyShareDb saveKeyShare(ee.cyber.cdoc2.client.model.KeyShare dto) {
        return this.shareRepository.save(
            new KeyShareDb()
                .setRecipient(dto.getRecipient())
                .setShare(dto.getShare())
        );
    }

}
