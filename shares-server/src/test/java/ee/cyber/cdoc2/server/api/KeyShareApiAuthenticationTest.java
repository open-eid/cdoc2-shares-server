package ee.cyber.cdoc2.server.api;

import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.NativeWebRequest;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

import ee.cyber.cdoc2.server.KeyShareIntegrationTest;
import ee.cyber.cdoc2.server.ValidateAuthToken;
import ee.cyber.cdoc2.server.ValidateSessionToken;
import ee.cyber.cdoc2.server.config.AuthCertificateConfigProperties;
import ee.cyber.cdoc2.server.config.NonceConfigProperties;
import ee.cyber.cdoc2.server.config.RpServerConfigProperties;
import ee.cyber.cdoc2.server.config.RpServerJwkConf;
import ee.cyber.cdoc2.server.model.entity.KeyShareDb;
import ee.cyber.cdoc2.server.model.entity.KeyShareNonceDb;
import ee.cyber.cdoc2.server.model.repository.KeyShareNonceRepository;
import ee.cyber.cdoc2.server.model.repository.KeyShareRepository;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class KeyShareApiAuthenticationTest extends KeyShareIntegrationTest {

    private static final byte[] SHARE = new byte[128];
    private static final String SID_DEMO_IDENTIFIER = "40504040001";
    private static final String ETSI_RECIPIENT = "etsi/PNOEE-" + SID_DEMO_IDENTIFIER;
    private static final String SHARE_ID = "ff0102030405060708090a0b0c0e0dff";
    private static final byte[] NONCE_BYTES = HexFormat.of().parseHex("000102030405060708090a0b0c0e0dff");
    private static final int WIREMOCK_PORT = 9090;

    private static final String SESSION_TOKEN_WITH_FILTERED_DISCLOSURES_BASE64URL =
        "Session token validation mocked";
    private static final String SESSION_TOKEN_SIGNING_CERTIFICATE_BASE64URL =
        "Session token certificate validation mocked";

    // pre-generated using cdoc2-java-ref-impl AuthTokenCreatorTest::testCreateAuthToken test
    // generated with SID demo env
    @SuppressWarnings("checkstyle:LineLength")
    private static final String AUTH_TOKEN_WITH_FILTERED_DISCLOSURES_BASE64URL =
        "eyJ0eXAiOiJ2bmQuY2RvYzIuYXV0aC10b2tlbi52MStzZC1qd3QiLCJhbGciOiJSU0FTU0EtUFNTK0FDU1BfVjIifQ.eyJpc3MiOiJldHNpL1BOT0VFLTQwNTA0MDQwMDAxIiwiX3NkIjpbInhiN244cURmRmNYYjB0aUg4WldNaTlyM0pVUmhhX3hkNGdJTVRMVWp2SFUiXSwiX3NkX2FsZyI6InNoYS0yNTYifQ.CMp6BaBsuVYPgmHi9aNWWQEXy4A6bHDacCigV7OQvO8obNuldfV8slpchp_ytwMtZ-pb5KbdvBNGMS4kF2LQXf68Vl8RRCFIMRCovU7LaBIVRhU6YW0XPFPRGD2UBmxKUnWKpGR3Tolf1wtZZ_Zi5ThmnHCAEbaKrIqYqNMFKCqTRrC0fUwWR0MVEDdQ_3vXtlboiT1QM3N-k5YCvTvjWBnY7CSqaeIYXHvH3Jq2pKMZAOtmYB3a8tPGvegw7KcQdBGKoMkEG3FQjshxDLRurMKPlhVlmBs6FQVF-eZZDxm9ichH1ycg3jzKZDivoYqmQd59hoLbNR6eGSrIt58-sdLVVbGPJX1mUem2QioZ3x24PnsSSrsusQCrlcca8QJA0VrDPFg6U0aq60GShdFLt2Avhz1xXCun0EBmMPoGs37vGVyfP-r7KqKFbxTDZ6Nb3hGjll3B9UvkdmIa_nBOZQv-Azzcnj9rGeI_qGzspsHSCu9Cz3LbA-q1_kEEJlm4c-8kbbvrujc1lZLXY6rZDnymRgfKjSNCK1TJAqQvKsFtWADmBVkg9uOiu_F_dmjZ6pPzFhQ_IvAxEahgbz_3wRPSFs2iauV31-duShO5eJydEItRrx4srAGtrSAyiUnn1ZdK22f5RBvjpKCBqXCHzKrf0tNSFqRR1s7-Vcerktv4d_bXMwRYuyMMEAbH075ETOFQ6eP0Bpdl7ne3_esH2T6Mga4l8niRpzDYxt987vP95rVUAEc3TmjPmGzp1jTERUaoYnFqFeX6nrJyaTOED0PBi72VmAdpgXIfZ2CxJrb_PytoATUDMiBc27hYDmwFox9WfovE5MU7VMfjjqXcUVrZD7IlAeYsHnU46y_kcXsEITgK_Kz--P3fvKYd8PoNPiWyN2jg-w4fa6fuU7hWbYNTNA4qW_CFHcMJou9T_9vpfEOMJ9xWi7YcUzu-39vFHi1FNQ1V2QP5Uqrq2lhBJ6xQTah-McNBOw1gWsl1U2JFci358My05X4yGAxheG6C~WyJJX21LbHJRZ19RMWVTNmw4bUJHSDNRIiwiYXVkIixbeyIuLi4iOiJDbTVpRzFTT0wyZTdvcUhqejdnOVBSTW5IMVNlQzl4dGpQVDJOeVFwdENJIn0seyIuLi4iOiI1TWNpZjRGUHVaNWhLdDFrTnRJbExtNmtIVk1iZEV1MzZsZFVnWTRRU19rIn1dXQ~WyJfYnNfSHFVc0szanNBak5xYUNLTFFnIiwiaHR0cHM6Ly9sb2NhbGhvc3Q6ODQ0My9rZXktc2hhcmVzL2ZmMDEwMjAzMDQwNTA2MDcwODA5MGEwYjBjMGUwZGZmP25vbmNlPU1ESSJd~";
    @SuppressWarnings("checkstyle:LineLength")
    private static final String SIGNATURE_VALIDATION_PARAMS_BASE64URL =
        "eyJpbnRlcmFjdGlvbnNEaWdlc3QiOiI0QTNRS0Rxam1xQXR4Rmo2U2lZNWtiVHBMYURZN3BQRnAzb0RBNXFBYy84PSIsImludGVyYWN0aW9uVHlwZVVzZWQiOiJjb25maXJtYXRpb25NZXNzYWdlQW5kVmVyaWZpY2F0aW9uQ29kZUNob2ljZSIsInNpZ25hdHVyZSI6eyJzZXJ2ZXJSYW5kb20iOiJBcmRTanNUTlFnYkRNNFdza2ttWUFJU1UiLCJ1c2VyQ2hhbGxlbmdlIjoiRmI0VGtKUmZTbDdoaGZBSElpc0lZaVhzWDFRTjhyVEg4RHdoTnNKeC1hZyIsInNpZ25hdHVyZUFsZ29yaXRobSI6InJzYXNzYS1wc3MiLCJmbG93VHlwZSI6Ik5vdGlmaWNhdGlvbiIsInNpZ25hdHVyZUFsZ29yaXRobVBhcmFtZXRlcnMiOnsiaGFzaEFsZ29yaXRobSI6IlNIQS0yNTYiLCJtYXNrR2VuQWxnb3JpdGhtIjp7ImFsZ29yaXRobSI6ImlkLW1nZjEiLCJwYXJhbWV0ZXJzIjp7Imhhc2hBbGdvcml0aG0iOiJTSEEtMjU2In19LCJzYWx0TGVuZ3RoIjozMiwidHJhaWxlckZpZWxkIjoiMHhiYyJ9fX0=";
    @SuppressWarnings("checkstyle:LineLength")
    private static final String AUTH_TOKEN_SIGNING_CERTIFICATE_BASE64URL =
        "MIIGpzCCBi6gAwIBAgIQGcJUbe6JHI6jJyV-42vjnTAKBggqhkjOPQQDAzBxMSwwKgYDVQQDDCNURVNUIG9mIFNLIElEIFNvbHV0aW9ucyBFSUQtUSAyMDI0RTEXMBUGA1UEYQwOTlRSRUUtMTA3NDcwMTMxGzAZBgNVBAoMElNLIElEIFNvbHV0aW9ucyBBUzELMAkGA1UEBhMCRUUwHhcNMjYwMTA2MTQyNTAxWhcNMjkwMTA1MTQyNTAwWjBXMQswCQYDVQQGEwJFRTEQMA4GA1UEAwwHVEVTVCxPSzENMAsGA1UEBAwEVEVTVDELMAkGA1UEKgwCT0sxGjAYBgNVBAUTEVBOT0VFLTQwNTA0MDQwMDAxMIIDIjANBgkqhkiG9w0BAQEFAAOCAw8AMIIDCgKCAwEAkI98VzyaeSueyaUQYIXMMf-1VY10Gw-b8Q13Rb9N62ROZY97wMIB__f8_PuOIoqkAPM6Tn_t4lp1R_rHrbuqs0hl2dgLlOcR5wmWmp7YfKPDvRndVLl_doIHruxY8O60rFGskSnqt4coHN4xGcmCyPkJoB8Rfm8-Y9poVKAreS0Ta32p5OSME0HjSs7-ahB2erWfb2GulFw1vyeH42d3XDpCCfd6CByvSsi4oByUqs5G-kjSrGUglflgWXK3MxBYto0swgsbD1nrW5doU_cMCfRoFURun4XguX8dTt9VeyqeJitxRfub2Hj18RbsKuoFNHQNOxAxRK4oTVCtUrYbVqBHDmoOm8r3CsSuqjuZ2njQybiUhBofpTVMCZ6lB6VgoLphmEwSEOQXIumpmpb2qJZqbZaBoyyWb4f5AQjw3Q5lwPSao5215hIgSuuENRezpP9rTzIwyOMbnV2nMSMInAuaXIXskB2NdpMsROsvOqBC0h5azTj9naCS-5EW-9eI7GGK03Du5JoKD5wYajJxfcxFwBAl8Ko71OvhGFtYiu-hqzz-CyG6NswB87KvzDYUCQ-0qOfgRBNCgYnbjnuYVJb3CGLp_cP5GmKtUC3wHX1WnPGyK4bD19Rcy-FhG6mD_ZrAPcmZ3s4FLLErpRJ3ui-fiMPLQl2bpCKTWoaEZoPg6Grnhr3bE2ZiKWmqdVwf30bG3-GnvTBTuF0T1lzt6NeBlB23SJsffCmzSFSNcFJHHYI1FYdZu2p0gL6KAabEmnE8GrTrCn93DFNBtoKu9vG30QrRzyh-itPvtn9w-9t-nDkhaVHmNCjWD1xcMeXsyK8ek0rbz5aVe_RPvCifhIpgjqNsDHh9q1QT9KIFsd6RD2XPMlekL9c6YiVY9H7uRyIQWqJwtrvNvBKj4ZT9745zTfkhCJTPvnLy-4iKeINVZ2f98BblsGAEHKGol8YA-3SRkPh9BVnVhSdI3lxCDEbmHuk21GIPE9689efSvbcDEHpqeYoxo3tXjl_hqfzPAgMBAAGjggH1MIIB8TAJBgNVHRMEAjAAMB8GA1UdIwQYMBaAFLAkFxmI42b4zShYZXtNFNiSZk9rMHAGCCsGAQUFBwEBBGQwYjAzBggrBgEFBQcwAoYnaHR0cDovL2Muc2suZWUvVEVTVF9FSUQtUV8yMDI0RS5kZXIuY3J0MCsGCCsGAQUFBzABhh9odHRwOi8vYWlhLmRlbW8uc2suZWUvZWlkcTIwMjRlMDAGA1UdEQQpMCekJTAjMSEwHwYDVQQDDBhQTk9FRS00MDUwNDA0MDAwMS1ERU0wLVEweAYDVR0gBHEwbzBjBgkrBgEEAc4fEQIwVjBUBggrBgEFBQcCARZIaHR0cHM6Ly93d3cuc2tpZHNvbHV0aW9ucy5ldS9yZXNvdXJjZXMvY2VydGlmaWNhdGlvbi1wcmFjdGljZS1zdGF0ZW1lbnQvMAgGBgQAj3oBAjAoBgNVHQkEITAfMB0GCCsGAQUFBwkBMREYDzE5MDUwNDA0MTIwMDAwWjAWBgNVHSUEDzANBgsrBgEEAYPmYgUHADA0BgNVHR8ELTArMCmgJ6AlhiNodHRwOi8vYy5zay5lZS90ZXN0X2VpZC1xXzIwMjRlLmNybDAdBgNVHQ4EFgQUX9YaVGlPdUOO2J6rzNc4sljBQBAwDgYDVR0PAQH_BAQDAgeAMAoGCCqGSM49BAMDA2cAMGQCMHhYJCeKceJv_m0xcFRssS4WVFnnCryDiuSEpjDZu0irJ_XurXXIFDr-9hhl2x7GMwIwbiD5GALRtwzUaEh-SV9jigT9Oc336f6QYf8YaSA0-Un8eRQPa9wTK0cSQrM_CUIu";

    private static final String CS_RP_SIGNED_HASH = "sj2RtSo7c1tx+J00KWWkzyv4iQ2L2cuX0InnFFi+GAQ=";
    private static final String CS_RP_NAME = "DEMO";
    private static final String CS_SIGNATURE_INPUT =
        "rp-sig=(\"x-rp-signed-hash\" \"x-rp-name\");created=1779011296;keyid=\"rp-server-ec-key-2026\"";
    private static final String CS_SIGNATURE =
        "rp-sig=:nt5aITnpc8JjVrOYw8q46bNieq9L7y8gBjw+rJJ7BoY4X3h8BL5PwwcUBzl70iTOvikGCBOmpjbDY1661EqMMA==:";

    @Mock
    private KeyShareRepository mockShareRep;

    @Mock
    private KeyShareNonceRepository mockNonceRep;

    @Mock
    private NativeWebRequest mockNativeWebRequest;

    @Mock
    private HttpServletRequest mockHttpServletRequest;

    @Mock
    private ValidateSessionToken mockValidateSessionToken;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private SslBundles sslBundles; // initialized from application.properties

    @Autowired
    private RpServerJwkConf rpServerJwkConf;

    private KeyShareApiService keyShareApiService;

    @RegisterExtension
    static WireMockExtension wiremock = WireMockExtension.newInstance()
        .options(wireMockConfig().port(WIREMOCK_PORT))
        .build();

    @Test
    void contextLoads() {
        // tests that test is configured properly (no exceptions means success)
        // In case configuration errors, spring fails run-time during initialization
        Assertions.assertNotNull(sslBundles); // from application.properties
    }

    @BeforeEach
    public void setUp() throws IOException {
        keyShareApiService = new KeyShareApiService(
            new AuthCertificateConfigProperties(),
            mockNativeWebRequest,
            mockShareRep,
            mockNonceRep,
            mockValidateSessionToken,
            new ValidateAuthToken(
                sslBundles,
                new AuthCertificateConfigProperties(),
                new RpServerConfigProperties(),
                mockNonceRep,
                new NonceConfigProperties(),
                rpServerJwkConf
            )
        );

        Resource resource = resourceLoader.getResource("classpath:rp-server-well-known.json");
        String keysResponseBody = resource.getContentAsString(StandardCharsets.UTF_8);

        wiremock.stubFor(
            WireMock.get(urlEqualTo("/.well-known/jwks.jws"))
                .willReturn(aResponse()
                    .withStatus(HttpStatus.OK.value())
                    .withBody(keysResponseBody)
                )
        );
    }

    @Test
    void shouldAuthenticateAndGetKeyShare() {
        KeyShareDb keyShareDb = new KeyShareDb()
            .setShareId(SHARE_ID)
            .setShare(SHARE)
            .setRecipient(ETSI_RECIPIENT);

        KeyShareNonceDb nonceDb = new KeyShareNonceDb()
            .setShareId(SHARE_ID)
            .setNonce(NONCE_BYTES)
            .setId(1L);
        nonceDb.setCreatedAt(Instant.now());

        when(mockNativeWebRequest.getNativeRequest(HttpServletRequest.class)).thenReturn(mockHttpServletRequest);
        when(mockHttpServletRequest.getHeader("X-Forwarded-Proto")).thenReturn("https");
        when(mockHttpServletRequest.getHeader("X-Forwarded-Host")).thenReturn("localhost");
        when(mockHttpServletRequest.getHeader("X-Forwarded-Port")).thenReturn("8443");
        when(mockHttpServletRequest.getRequestURI()).thenReturn("/key-shares/" + SHARE_ID);

        when(mockShareRep.findById(SHARE_ID)).thenReturn(Optional.of(keyShareDb));
        when(mockNonceRep.findByShareIdAndNonce(eq(SHARE_ID), any())).thenReturn(Optional.of(nonceDb));

        var resp = keyShareApiService.getKeyShareByShareId(
            SHARE_ID,
            AUTH_TOKEN_WITH_FILTERED_DISCLOSURES_BASE64URL,
            AUTH_TOKEN_SIGNING_CERTIFICATE_BASE64URL,
            SESSION_TOKEN_WITH_FILTERED_DISCLOSURES_BASE64URL,
            SESSION_TOKEN_SIGNING_CERTIFICATE_BASE64URL,
            SIGNATURE_VALIDATION_PARAMS_BASE64URL,
            CS_RP_SIGNED_HASH,
            CS_RP_NAME,
            CS_SIGNATURE_INPUT,
            CS_SIGNATURE
        );

        assertTrue(resp.getStatusCode().is2xxSuccessful());
        assertTrue(resp.hasBody());
        assertEquals(ETSI_RECIPIENT, resp.getBody().getRecipient());
        assertArrayEquals(SHARE, resp.getBody().getShare());
    }

}
