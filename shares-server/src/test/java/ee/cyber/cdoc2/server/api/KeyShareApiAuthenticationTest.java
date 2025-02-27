package ee.cyber.cdoc2.server.api;

import jakarta.servlet.http.HttpServletRequest;

import com.nimbusds.jose.util.X509CertUtils;
import ee.cyber.cdoc2.server.KeyShareIntegrationTest;
import ee.cyber.cdoc2.server.config.AuthCertificateConfigProperties;
import ee.cyber.cdoc2.server.config.NonceConfigProperties;
import ee.cyber.cdoc2.server.model.entity.KeyShareDb;
import ee.cyber.cdoc2.server.model.entity.KeyShareNonceDb;
import ee.cyber.cdoc2.server.model.repository.KeyShareNonceRepository;
import ee.cyber.cdoc2.server.model.repository.KeyShareRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ssl.SslBundles;

import org.springframework.web.context.request.NativeWebRequest;

import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class KeyShareApiAuthenticationTest extends KeyShareIntegrationTest {

    private static final byte[] SHARE = new byte[128];
    private static final String SID_DEMO_IDENTIFIER = "30303039914";
    private static final String ETSI_RECIPIENT = "etsi/PNOEE-" + SID_DEMO_IDENTIFIER;
    private static final String SHARE_ID = "ff0102030405060708090a0b0c0e0dff";
    private static final byte[] NONCE_BYTES = HexFormat.of().parseHex("000102030405060708090a0b0c0e0dff");

    @Mock
    private KeyShareRepository mockShareRep;

    @Mock
    private KeyShareNonceRepository mockNonceRep;

    @Mock
    private NativeWebRequest mockNativeWebRequest;

    @Mock
    private HttpServletRequest mockHttpServletRequest;

    @Autowired
    private SslBundles sslBundles; // initialized from application.properties

    private KeyShareApiService keyShareApiService;

    // pre-generated using cdoc2-java-ref-impl AuthTokenCreatorTest::testCreateAuthToken test
    // generated with SID demo env
    private static final String AUTH_TICKET = """
        eyJ0eXAiOiJ2bmQuY2RvYzIuYXV0aC10b2tlbi52MStzZC1qd3QiLCJhbGciOiJSUzI1NiJ9
        .
        eyJpc3MiOiJldHNpL1BOT0VFLTMwMzAzMDM5OTE0IiwiX3NkIjpbIlZQYmtIX3ZUTGh3WEcxYWZYMXUtTTljU21HZnZNZnR4b0NDNzlpMTJSUG8
        iXSwiX3NkX2FsZyI6InNoYS0yNTYifQ
        .
        II2r_TcyfZoO0zu_ltN4Gd-FYvcP-dzRuiGHOVdMVgBzYLws2nyuYrtQcthETce4EvgE6-r-TaFtSfZj7BcLvQwMX84xEwbHIDUmIxl2GMYK1u4
        4HpK9N1QnNDTssvrMm3isyRL65dSm9siUGLZVa4sTHpR7CoUjGfpGg8NfGQjoYjdGIEsd4MuCfTKJGqFCh-W5emLf7T08RPCphcUemq028C7ZrB
        B7-7Z8QP4XKC-yBprru9diwibALAjeuyo5lHa7vs5tu_glWs895wpIze8CYhmPadbAijOFrCP7r-KRV6V7wpl5l1DU9bXEJT5QQWdOUWJq02VYC
        xORFmL1y6hy4ZiTFi9JoD389WWQksy8JmZKT7sFkNHt5lSuCsZEl-LoxpAxnQ0udMlx1tvZ4NMOqEruCyw0GJ8DHBWQx5fLGbkOriy2-QltBmms
        HUKXHIVYubwWOgHbcp8HMCBakaWxxzMXJA_Xjo5FhNAVQ2fLPhXejq3t4e_SWc-We04QdptepP2CnMNn_YIzcdausPdmu3LOcYKPe3bT5wTmnr8
        lv_Xk0eLADHKjhRQRdNQeHXcNPJ1zgAZbPOsldgXgu__uKgMwG3lrfUI9HBJ9H1XbG_nUZsFEKT-5_pct_soSWgDV8HauhkWV1qCGBqrHR_NrqF
        S9wi-dAzEnPihUyaXhFNjpQuI2srsGU4CMXooAjqh7UJxO8hEZezkvN4SWZ62MigIfa9-7msV5pdrv5AvqEyDmW7syddqvdXoWGXRi3GsX9WXYL
        pop_CUOa2O1ZIva9yzIHTA_RAoE7fUB9hsyq4ibaeLy25czuaRG8Ziv8GWR9DFf90Hvx1Cdg7qoa6FpqjJrLW3A5kVdaW-ebG1sb2T2Fk0kVibg
        7TAajHKZ2GXO0kK8OJT-L9EC3703kdB4qQpw3Dcqwe5Nptoyqhl9PkbKqja-LeDMbvwvflT5X-vbGhiydmM3cLmjBti0XsBAHm2NGZpg4arJ11x
        w5156FFrSOZvDA6oCIKYUpqcV
        ~
        WyJzOTVWZ3ZWRGFSY09qM0hXa2I3YlhRIiwiYXVkIixbeyIuLi4iOiJCbEtoRnJSZlVjM3hLRlFmU2xZUzQyaWxsbXVoS2YzZUNKa2EyYXNfRlR
        vIn0seyIuLi4iOiJtellSVk9lTmROZl9uXzB3ZVdSYTg3NUtZYnRINTFRVEowWUtHRm1acUZNIn1dXQ
        ~
        WyJYU2ZPUkpYdGZuTXk1aUZ2bFZMaGd3IiwiaHR0cHM6Ly9sb2NhbGhvc3Q6ODQ0My9rZXktc2hhcmVzL2ZmMDEwMjAzMDQwNTA2MDcwODA5MGE
        wYjBjMGUwZGZmP25vbmNlXHUwMDNkQUFFQ0F3UUZCZ2NJQ1FvTERBNE5fdyJd
        ~
        """.replaceAll("\\s", ""); //remove all whitespace

    // SID demo env cert for 30303039914 that automatically authenticates successfully
    private final String sidCertStr = """
        -----BEGIN CERTIFICATE-----
        MIIIIjCCBgqgAwIBAgIQUJQ/xtShZhZmgogesEbsGzANBgkqhkiG9w0BAQsFADBoMQswCQYDVQQGEwJFRTEiMCAGA1UECgwZ
        QVMgU2VydGlmaXRzZWVyaW1pc2tlc2t1czEXMBUGA1UEYQwOTlRSRUUtMTA3NDcwMTMxHDAaBgNVBAMME1RFU1Qgb2YgRUlE
        LVNLIDIwMTYwIBcNMjQwNzAxMTA0MjM4WhgPMjAzMDEyMTcyMzU5NTlaMGMxCzAJBgNVBAYTAkVFMRYwFAYDVQQDDA1URVNU
        TlVNQkVSLE9LMRMwEQYDVQQEDApURVNUTlVNQkVSMQswCQYDVQQqDAJPSzEaMBgGA1UEBRMRUE5PRUUtMzAzMDMwMzk5MTQw
        ggMiMA0GCSqGSIb3DQEBAQUAA4IDDwAwggMKAoIDAQCo+o1jtKxkNWHvVBRA8Bmh08dSJxhL/Kzmn7WS2u6vyozbF6M3f1lp
        XZXqXqittSmiz72UVj02jtGeu9Hajt8tzR6B4D+DwWuLCvTawqc+FSjFQiEB+wHIb4DrKF4t42Aazy5mlrEy+yMGBe0ygMLd
        6GJmkFw1pzINq8vu6sEY25u6YCPnBLhRRT3LhGgJCqWQvdsN3XCV8aBwDK6IVox4MhIWgKgDF/dh9XW60MMiW8VYwWC7ONa
        3LTqXJRuUhjFxmD29Qqj81k8ZGWn79QJzTWzlh4NoDQT8w+8ZIOnyNBAxQ+Ay7iFR4SngQYUyHBWQspHKpG0dhKtzh3zELIk
        o8sxnBZ9HNkwnIYe/CvJIlqARpSUHY/Cxo8X5upwrfkhBUmPuDDgS14ci4sFBiW2YbzzWWtxbEwiRkdqmA1NxoTJybA9Frj6
        NIjC4Zkk+tL/N8Xdblfn8kBKs+cAjk4ssQPQruSesyvzs4EGNgAk9PX2oeelGTt02AZiVkIpUha8VgDrRUNYyFZc3E3Z3Ph1
        aOCEQMMPDATaRps3iHw/waHIpziHzFAncnUXQDUMLr6tiq+mOlxYCi8+NEzrwT2GOixSIuvZK5HzcJTBYz35+ESLGjxnUjb
        ssfra9RAvyaeE1EDfAOrJNtBHPWP4GxcayCcCuVBK2zuzydhY6Kt8ukXh5MIM08GRGHqj8gbBMOW6zEb3OVNSfyi1xF8MYAT
        KnM1XjSYN49My0BPkJ01xCwFzC2HGXUTyb8ksmHtrC8+MrGLus3M3mKFvKA9VatSeQZ8ILR6WeA54A+GMQeJuV54ZHZtD208
        5Vj7R+IjR+3jakXBvZhVoSTLT7TIIa0U6L46jUIHee/mbf5RJxesZzkP5zA81csYyLlzzNzFah1ff7MxDBi0v/UyJ9ngFCeL
        t7HewtlC8+HRbgSdk+57KgaFIgVFKhv34Hz1Wfh3ze1Rld3r1Dx6so4h4CZOHnUN+hprosI4t1y8jorCBF2GUDbIqmBCx7Dg
        qT6aE5UcMcXd8CAwEAAaOCAckwggHFMAkGA1UdEwQCMAAwDgYDVR0PAQH/BAQDAgSwMHkGA1UdIARyMHAwZAYKKwYBBAHOHw
        MRAjBWMFQGCCsGAQUFBwIBFkhodHRwczovL3d3dy5za2lkc29sdXRpb25zLmV1L3Jlc291cmNlcy9jZXJ0aWZpY2F0aW9uLX
        ByYWN0aWNlLXN0YXRlbWVudC8wCAYGBACPegECMB0GA1UdDgQWBBQUFyCLUawSl3KCp22kZI88UhtHvTAfBgNVHSMEGDAWgB
        SusOrhNvgmq6XMC2ZV/jodAr8StDATBgNVHSUEDDAKBggrBgEFBQcDAjB8BggrBgEFBQcBAQRwMG4wKQYIKwYBBQUHMAGGHW
        h0dHA6Ly9haWEuZGVtby5zay5lZS9laWQyMDE2MEEGCCsGAQUFBzAChjVodHRwOi8vc2suZWUvdXBsb2FkL2ZpbGVzL1RFU1
        Rfb2ZfRUlELVNLXzIwMTYuZGVyLmNydDAwBgNVHREEKTAnpCUwIzEhMB8GA1UEAwwYUE5PRUUtMzAzMDMwMzk5MTQtTU9DSy
        1RMCgGA1UdCQQhMB8wHQYIKwYBBQUHCQExERgPMTkwMzAzMDMxMjAwMDBaMA0GCSqGSIb3DQEBCwUAA4ICAQCqlSMpTx+/n
        wfI5eEislq9rce9eOY/9uA0b3Pi7cn6h7jdFes1HIlFDSUjA4DxiSWSMD0XX1MXe7J7xx/AlhwFI1WKKq3eLx4wE8sjOaacH
        nwV/JSTf6iSYjAB4MRT2iJmvopgpWHS6cAQfbG7qHE19qsTvG7Ndw7pW2uhsqzeV5/hcCf10xxnGOMYYBtU7TheKRQtkeBiP
        Jsv4HuIFVV0pGBnrvpqj56Q+TBD9/8bAwtmEMScQUVDduXPc+uIJJoZfLlUdUwIIfhhMEjSRGnaK4H0laaFHa05+KkFtHzc/
        iYEGwJQbiKvUn35/liWbcJ7nr8uCQSuV4PHMjZ2BEVtZ6Qj58L/wSSidb4qNkSb9BtlK+wwNDjbqysJtQCAKP7SSNuYcEAWl
        mvtHmpHlS3tVb7xjko/a7zqiakjCXE5gIFUmtZJFbG5dO/0VkT5zdrBZJoq+4DkvYSVGVDE/AtKC86YZ6d1DY2jIT0c9Blb
        Fp40A4Xkjjjf5/BsRlWFAs8Ip0Y/evG68gQBATJ2g3vAbPwxvNX2x3tKGNg+aDBYMGM76rRrtLhRqPIE4Ygv8x/s7JoBxy1q
        Czuwu/KmB7puXf/y/BBdcwRHIiBq2XQTfEW3ZJJ0J5+Kq48keAT4uOWoJiPLVTHwUP/UBhwOSa4nSOTAfdBXG4NqMknYwvAE
        9g==
        -----END CERTIFICATE-----
        """;

    @Test
    void contextLoads() {
        // tests that test is configured properly (no exceptions means success)
        // In case configuration errors, spring fails run-time during initialization
        Assertions.assertNotNull(sslBundles); // from application.properties
    }

    @BeforeEach
    public void setUp() {
        keyShareApiService = new KeyShareApiService(
            new AuthCertificateConfigProperties(),
            new NonceConfigProperties(),
            mockNativeWebRequest,
            mockShareRep,
            mockNonceRep,
            sslBundles
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

        String pemCertNoLineBreaks = X509CertUtils.toPEMString(X509CertUtils.parse(sidCertStr), false);
        var resp = keyShareApiService.getKeyShareByShareId(SHARE_ID, AUTH_TICKET, pemCertNoLineBreaks);

        assertTrue(resp.getStatusCode().is2xxSuccessful());
        assertTrue(resp.hasBody());
        assertEquals(ETSI_RECIPIENT, resp.getBody().getRecipient());
        assertArrayEquals(SHARE, resp.getBody().getShare());
    }

}
