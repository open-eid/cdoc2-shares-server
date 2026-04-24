package ee.cyber.cdoc2.server;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

import ee.cyber.cdoc2.client.Cdoc2KeySharesApiClient;
import ee.cyber.cdoc2.client.api.ApiException;
import ee.cyber.cdoc2.client.model.KeyShare;
import ee.cyber.cdoc2.client.model.NonceResponse;
import ee.cyber.cdoc2.server.config.MonitoringConfigProperties;
import ee.cyber.cdoc2.server.model.entity.KeyShareDb;
import ee.cyber.cdoc2.server.model.entity.SessionNonceDb;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;


@Slf4j
class KeyShareApiTests extends KeyShareIntegrationTest {

    @SuppressWarnings("checkstyle:LineLength")
    private static final String SESSION_TOKEN_WITH_FILTERED_DISCLOSURES_BASE64URL =
        "eyJraWQiOiJlYy1rZXktMjAyNiIsInR5cCI6InZuZC5jZG9jMi5zZXNzaW9uLXRva2VuLnYyK3NkLWp3dCIsImFsZyI6IkVTMjU2In0.eyJycENoYWxsZW5nZSI6InJrZ2s0cTE2eTYxbFJoRVJOcVZVdzBpdEhXZ3MzbWZMS3Y5cEQ2Z2xCdDl0ZDJRbmVhd1lLM0ZGOHFkMXBSakdDbnlyZ3NWTmprSXJ3T3NuZXkzOXl3PT0iLCJzdWIiOiJldHNpL1BOT0VFLTQwNTA0MDQwMDAxIiwic2lnbmF0dXJlIjp7InZhbHVlIjoiSlhtVmgwWlZqdFRUZHFFaHlET1NCejNMLyt4UHZPTGF1WXVmbmUydS8wRkVuZ0loQ0g4WEl6ZW5zazhsa3BLZlNxYVBjSzZReDF5bVZpdGM1YUNWY1N6bjRzUVV3SW5OODBXVEd1UTZtNTNESGdXWnFnS3NabHErSDcwamxxMXZSUE0vS3UwVEJIK01GRkhOeWp5SWZWN0MwOVMyK2pCbm1kYWEzM0FaNCtnOS9FNWVnL1p6QktHQWNnQmFyU01lYVpOdXNXQWNrdlJBQ3Y1WXlidUVYK0JSM3NYMXA0U09XNWdMT2lPOUwzaWtzVXNzVFp2K3MyVU9kbFVZTFpTVGVrWWIrYXFQRkdzODdjY1FQUmZuRlRWV1EwZ0pvVENWVG1lWWdXazA5MjZMOVREaUlQZlJ5cWxBbkNQNWRPZGlxRmNLSXFOZ3d4Z0RyZmlITmJ2R3hhQ3hGeVdJd2R2ekdmNEZPa3VMaVdndXVPQVo3YlgxVzIwQnNlZjFHTnRTQUdBVlRMbmJsMUNiOC8rT2dwRU52WXM2UUtxdXdiRlZHTDhzRDJEc1czNXZodllXdkJJN0tVK1NxODBnSDhURkFvdzNiN3QzOU80UEJmbVMzOUJrRTgxMW9mK2MwSFpNMXhJd3NTajZVVkR3bkNLOHYwZ1BKOGZMMmo5NDIrVUVVRUpQUEJaNmo1TWcrRmxBb1ZFR2pHakp1cDF3NVdCTFVBYTl1blRiNWp5UGtFUW84clVrS1Y0ZmQ0b21XTEpobGJXcmNYWlh5ZndrRGhJR211Uzh4Z2w2NSs0anMyVTI4THRDQzJYSjhlNWJhWm9rNWQ0Q2VVbFJvUjErbmIvZTg0cmthOUtPOUV6MGZHVmRlSmc0MFZhYTBWb2xBeDkxYVVmUS9mV0tpRXMxbHBOR0EyMG55cytJSTU3QWZqRDdkdGJxTzZaNzBXbEpUenMrREE0SHJEcXFQQ0ZRbVRmc2VhWFFOOVBxK3RnRFZqdFc1TlVRMTg4UlhSZ2pvalp4ZitCT2piVDJ6b0xxMS96VmdETkZTaW1kSElLQitJYUlwaDB5LzZHWWFGb0p2eERsRm9YSzhwUnU4My8vdGNnZmFuN1gzUWZKMnF1WTN3T2VDUHN5dmM1TklNdHRJdnhHRFlTdWI3QW9Ta3Z4eGFsdHg3Vy9FWEJWVmNDaWROZE1YM1ErZ2VNQlMzeS9NbEE3M0pqMXQxb1N6UmZxdFFpY2tYd0w3bmYrNXB5RzY2eUVFb1ZaZ1dVdE0zMUptSE9LVUF6UGVFOSthd2xRUjd3NXJSeitFemJ1MitLdlY2N1V4NFdXVG5JY0pOUG4rRkhOKzE1V1ZlTVhuRlhxVHZOSiIsInNlcnZlclJhbmRvbSI6InNWOXdsS3RaZTV0cjBnTjlpZXRQU0ovVCIsInVzZXJDaGFsbGVuZ2UiOiJmeWtaTHJmU2tsMW9uMXBITlBrZEFZUS1pekd0N1ZGeWN6eUNDM2x4cmlrIiwic2lnbmF0dXJlQWxnb3JpdGhtIjoicnNhc3NhLXBzcyIsImZsb3dUeXBlIjoiTm90aWZpY2F0aW9uIiwic2lnbmF0dXJlQWxnb3JpdGhtUGFyYW1ldGVycyI6eyJoYXNoQWxnb3JpdGhtIjoiU0hBLTI1NiIsIm1hc2tHZW5BbGdvcml0aG0iOnsiYWxnb3JpdGhtIjoiaWQtbWdmMSIsInBhcmFtZXRlcnMiOnsiaGFzaEFsZ29yaXRobSI6IlNIQS0yNTYifX0sInNhbHRMZW5ndGgiOjMyLCJ0cmFpbGVyRmllbGQiOiIweGJjIn19LCJpc3MiOiJodHRwczovL2Nkb2MyLWF1dGgtc2VydmVyLmVlIiwic2NoZW1lTmFtZSI6InNtYXJ0LWlkLWRlbW8iLCJzaWduYXR1cmVQcm90b2NvbCI6IlJTQVNTQS1QU1MrQUNTUF9WMiIsIl9zZCI6WyJuTEpHdS05X3lKMmlEOGhrRXU5Ym5yc0EzUHJ5Y3UwVVE1WXQ5UENTNV8wIl0sImludGVyYWN0aW9uc0RpZ2VzdCI6Im9sSk43T1hVdmZ5MWJVUE51NzEyWDNBN01PbTFCWGlXdGxBbXYrdWJJejA9IiwiX3NkX2FsZyI6InNoYS0yNTYiLCJleHAiOjE3NzY4NzI1MjksImlhdCI6MTc3Njc4NjEyOSwiaW50ZXJhY3Rpb25UeXBlVXNlZCI6ImNvbmZpcm1hdGlvbk1lc3NhZ2VBbmRWZXJpZmljYXRpb25Db2RlQ2hvaWNlIiwicnBOYW1lIjoiREVNTyJ9.5ORVwgy0tMpX5tdwZmhnnQK_H4ngB-duofWj2OYCrJU5kL5dUvJRSeiC5QLbuzH-8gk08b5asqIW9lWNEErAuw~WyJONGFScHVxNTVRZzh6LTVxS3dlRURBIiwiYXVkIixbeyIuLi4iOiIxM19rVmNGcXF3M0tycllRaUpnUEJ4Qm1zOG1rY0puMmtnNWZBRGc4aUlFIn0seyIuLi4iOiJkaG9VbVZod0c2TEJIbkwyMHJxbDZFTkVFdjlfdHBPOFo4aUVFbmVESmhjIn1dXQ~WyJMb2dqR24xc21ZNmxpWllEZnh4OGhnIiwiaHR0cDovL2xvY2FsaG9zdDo4MDgwL3Nlc3Npb25fbm9uY2VfMi9uclZjU0VjSHVXdDJTS2Zqa01tNlJRIl0~";
    @SuppressWarnings("checkstyle:LineLength")
    private static final String SID_SIGNING_CERTIFICATE_BASE64URL =
        "MIIGpzCCBi6gAwIBAgIQGcJUbe6JHI6jJyV-42vjnTAKBggqhkjOPQQDAzBxMSwwKgYDVQQDDCNURVNUIG9mIFNLIElEIFNvbHV0aW9ucyBFSUQtUSAyMDI0RTEXMBUGA1UEYQwOTlRSRUUtMTA3NDcwMTMxGzAZBgNVBAoMElNLIElEIFNvbHV0aW9ucyBBUzELMAkGA1UEBhMCRUUwHhcNMjYwMTA2MTQyNTAxWhcNMjkwMTA1MTQyNTAwWjBXMQswCQYDVQQGEwJFRTEQMA4GA1UEAwwHVEVTVCxPSzENMAsGA1UEBAwEVEVTVDELMAkGA1UEKgwCT0sxGjAYBgNVBAUTEVBOT0VFLTQwNTA0MDQwMDAxMIIDIjANBgkqhkiG9w0BAQEFAAOCAw8AMIIDCgKCAwEAkI98VzyaeSueyaUQYIXMMf-1VY10Gw-b8Q13Rb9N62ROZY97wMIB__f8_PuOIoqkAPM6Tn_t4lp1R_rHrbuqs0hl2dgLlOcR5wmWmp7YfKPDvRndVLl_doIHruxY8O60rFGskSnqt4coHN4xGcmCyPkJoB8Rfm8-Y9poVKAreS0Ta32p5OSME0HjSs7-ahB2erWfb2GulFw1vyeH42d3XDpCCfd6CByvSsi4oByUqs5G-kjSrGUglflgWXK3MxBYto0swgsbD1nrW5doU_cMCfRoFURun4XguX8dTt9VeyqeJitxRfub2Hj18RbsKuoFNHQNOxAxRK4oTVCtUrYbVqBHDmoOm8r3CsSuqjuZ2njQybiUhBofpTVMCZ6lB6VgoLphmEwSEOQXIumpmpb2qJZqbZaBoyyWb4f5AQjw3Q5lwPSao5215hIgSuuENRezpP9rTzIwyOMbnV2nMSMInAuaXIXskB2NdpMsROsvOqBC0h5azTj9naCS-5EW-9eI7GGK03Du5JoKD5wYajJxfcxFwBAl8Ko71OvhGFtYiu-hqzz-CyG6NswB87KvzDYUCQ-0qOfgRBNCgYnbjnuYVJb3CGLp_cP5GmKtUC3wHX1WnPGyK4bD19Rcy-FhG6mD_ZrAPcmZ3s4FLLErpRJ3ui-fiMPLQl2bpCKTWoaEZoPg6Grnhr3bE2ZiKWmqdVwf30bG3-GnvTBTuF0T1lzt6NeBlB23SJsffCmzSFSNcFJHHYI1FYdZu2p0gL6KAabEmnE8GrTrCn93DFNBtoKu9vG30QrRzyh-itPvtn9w-9t-nDkhaVHmNCjWD1xcMeXsyK8ek0rbz5aVe_RPvCifhIpgjqNsDHh9q1QT9KIFsd6RD2XPMlekL9c6YiVY9H7uRyIQWqJwtrvNvBKj4ZT9745zTfkhCJTPvnLy-4iKeINVZ2f98BblsGAEHKGol8YA-3SRkPh9BVnVhSdI3lxCDEbmHuk21GIPE9689efSvbcDEHpqeYoxo3tXjl_hqfzPAgMBAAGjggH1MIIB8TAJBgNVHRMEAjAAMB8GA1UdIwQYMBaAFLAkFxmI42b4zShYZXtNFNiSZk9rMHAGCCsGAQUFBwEBBGQwYjAzBggrBgEFBQcwAoYnaHR0cDovL2Muc2suZWUvVEVTVF9FSUQtUV8yMDI0RS5kZXIuY3J0MCsGCCsGAQUFBzABhh9odHRwOi8vYWlhLmRlbW8uc2suZWUvZWlkcTIwMjRlMDAGA1UdEQQpMCekJTAjMSEwHwYDVQQDDBhQTk9FRS00MDUwNDA0MDAwMS1ERU0wLVEweAYDVR0gBHEwbzBjBgkrBgEEAc4fEQIwVjBUBggrBgEFBQcCARZIaHR0cHM6Ly93d3cuc2tpZHNvbHV0aW9ucy5ldS9yZXNvdXJjZXMvY2VydGlmaWNhdGlvbi1wcmFjdGljZS1zdGF0ZW1lbnQvMAgGBgQAj3oBAjAoBgNVHQkEITAfMB0GCCsGAQUFBwkBMREYDzE5MDUwNDA0MTIwMDAwWjAWBgNVHSUEDzANBgsrBgEEAYPmYgUHADA0BgNVHR8ELTArMCmgJ6AlhiNodHRwOi8vYy5zay5lZS90ZXN0X2VpZC1xXzIwMjRlLmNybDAdBgNVHQ4EFgQUX9YaVGlPdUOO2J6rzNc4sljBQBAwDgYDVR0PAQH_BAQDAgeAMAoGCCqGSM49BAMDA2cAMGQCMHhYJCeKceJv_m0xcFRssS4WVFnnCryDiuSEpjDZu0irJ_XurXXIFDr-9hhl2x7GMwIwbiD5GALRtwzUaEh-SV9jigT9Oc336f6QYf8YaSA0-Un8eRQPa9wTK0cSQrM_CUIu";
    private static final String SESSION_NONCE_FOR_TOKEN = "nrVcSEcHuWt2SKfjkMm6RQ";

    private static final byte[] SHARE = new byte[128];
    private static final String SHARE_RECIPIENT = "Recipient_for_key_share";
    private static final Instant INSTANT_NOW_SESSION_TOKEN_NOT_EXPIRED =
        Instant.parse("2026-04-22T12:30:00Z");
    private static final int WIREMOCK_PORT = 8090;

    @RegisterExtension
    static WireMockExtension wiremock = WireMockExtension.newInstance()
        .options(wireMockConfig().port(WIREMOCK_PORT))
        .build();

    @Autowired
    private MonitoringConfigProperties configProperties;

    @Autowired
    private ResourceLoader resourceLoader;

    private Cdoc2KeySharesApiClient client;

    @MockitoBean
    private Clock clock;

    @BeforeEach
    public void setup() throws Exception {
        client = createClient();

        Resource resource = resourceLoader.getResource("classpath:auth-server-well-known.json");
        String keysResponseBody = resource.getContentAsString(StandardCharsets.UTF_8);

        wiremock.stubFor(
            WireMock.get(urlEqualTo("/.well-known/jwks.jws"))
                .willReturn(aResponse()
                    .withStatus(HttpStatus.OK.value())
                    .withBody(keysResponseBody)
                )
        );

        saveNonceForSessionToken();
        when(clock.instant()).thenReturn(INSTANT_NOW_SESSION_TOKEN_NOT_EXPIRED);
    }

    @Test
    void shouldGetKeyShare() throws Exception {
        KeyShare keyShare = createKeyShare();
        keyShare.setRecipient(TestData.TEST_ETSI_RECIPIENT);

        String shareId = this.saveKeyShare(keyShare).getShareId();
        String nonce = client.createNonce(
            shareId,
            SESSION_TOKEN_WITH_FILTERED_DISCLOSURES_BASE64URL,
            SID_SIGNING_CERTIFICATE_BASE64URL
        ).getNonce();
        String xAuthTicket = TestData.generateTestAuthTicket(TestData.TEST_IDENTIFIER, baseUrl, shareId, nonce);

        Optional<KeyShare> response = client.getKeyShare(shareId, xAuthTicket,
            TestData.TEST_CERT_PEM,
            SESSION_TOKEN_WITH_FILTERED_DISCLOSURES_BASE64URL,
            SID_SIGNING_CERTIFICATE_BASE64URL,
            null
        );


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
            () -> client.getKeyShare(shareId, xAuthTicket, xAuthCert,
                SESSION_TOKEN_WITH_FILTERED_DISCLOSURES_BASE64URL,
                SID_SIGNING_CERTIFICATE_BASE64URL,
                null
            )
        );

        assertBadRequest(ex.getCode());
    }

    @Test
    void shouldFailWith400WhenGetKeyShareCertParamIsEmpty() throws Exception {
        KeyShare keyShare = createKeyShare();
        keyShare.setRecipient(TestData.TEST_ETSI_RECIPIENT);

        String shareId = this.saveKeyShare(keyShare).getShareId();
        String nonce = client.createNonce(
            shareId,
            SESSION_TOKEN_WITH_FILTERED_DISCLOSURES_BASE64URL,
            SID_SIGNING_CERTIFICATE_BASE64URL
        ).getNonce();
        String xAuthTicket = TestData.generateTestAuthTicket(TestData.TEST_IDENTIFIER, baseUrl, shareId, nonce);


        ApiException ex = assertThrows(
            ApiException.class,
            // null is checked by openapi generated code, but "" resulted NullPointerException, because
            // jose X509CertUtils.parseWithException("") returns null
            () -> client.getKeyShare(shareId, xAuthTicket, "",
                SESSION_TOKEN_WITH_FILTERED_DISCLOSURES_BASE64URL,
                SID_SIGNING_CERTIFICATE_BASE64URL,
                null
            )
        );

        assertBadRequest(ex.getCode());
    }

    @Test
    void shouldFailWith400WhenGetKeyShareAuthTicketParamIsEmpty() {
        KeyShare keyShare = createKeyShare();
        keyShare.setRecipient(TestData.TEST_ETSI_RECIPIENT);

        String shareId = this.saveKeyShare(keyShare).getShareId();

        ApiException ex = assertThrows(
            ApiException.class,
            () -> client.getKeyShare(shareId, "", TestData.TEST_CERT_PEM,
                SESSION_TOKEN_WITH_FILTERED_DISCLOSURES_BASE64URL,
                SID_SIGNING_CERTIFICATE_BASE64URL,
                null
            )
        );

        assertEquals(HttpStatus.BAD_REQUEST.value(), ex.getCode());
    }



    @Test
    void shouldFailToGetKeyShareWithNotFound() throws ApiException {
        String shareId = "SHARE_ID_MIN_LENGTH_SHOULD_BE_32";

        String nonce = "random";
        String xAuthTicket = TestData.generateTestAuthTicket(TestData.TEST_IDENTIFIER, baseUrl, shareId, nonce);

        Optional<KeyShare> keyShare = client.getKeyShare(shareId, xAuthTicket, TestData.TEST_CERT_PEM,
            SESSION_TOKEN_WITH_FILTERED_DISCLOSURES_BASE64URL,
            SID_SIGNING_CERTIFICATE_BASE64URL,
            null);

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
        System.out.println("Classpath");
        System.out.println(System.getProperty("java.class.path"));
        System.out.println("========================");

        var keyShare = createKeyShare();

        String shareId = this.saveKeyShare(keyShare).getShareId();

        NonceResponse response = client.createNonce(
            shareId,
            SESSION_TOKEN_WITH_FILTERED_DISCLOSURES_BASE64URL,
            SID_SIGNING_CERTIFICATE_BASE64URL
        );
        assertNotNull(response);
    }

    @Test
    void shouldFailToCreateKeyShareNonceWithNotFoundKeyShareId() {
        String shareId = "SHARE_ID_MIN_LENGTH_SHOULD_BE_32";

        ApiException ex = assertThrows(
            ApiException.class,
            () -> client.createNonce(
                shareId,
                SESSION_TOKEN_WITH_FILTERED_DISCLOSURES_BASE64URL,
                SID_SIGNING_CERTIFICATE_BASE64URL
            )
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

    private void saveNonceForSessionToken() {
        SessionNonceDb entity = new SessionNonceDb();
        entity.setNonce(Base64.getUrlDecoder().decode(SESSION_NONCE_FOR_TOKEN));
        entity.setCreatedAt(Instant.now());
        this.sessionNonceRepository.save(entity);
    }
}
