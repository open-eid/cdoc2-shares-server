package ee.cyber.cdoc2.server;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Properties;
import java.util.Set;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.X509CertUtils;

import ee.cyber.cdoc2.auth.AuthTokenCreator;
import ee.cyber.cdoc2.auth.EtsiIdentifier;
import ee.cyber.cdoc2.auth.SIDCertificateUtil;
import ee.cyber.cdoc2.auth.ShareAccessData;

import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Input test data utility class.
 */
@Slf4j
public final class TestData {

    //generated create-rsa-key-csr-crt-keystore.sh
    private static final String TEST_RSAKEY = """
-----BEGIN PRIVATE KEY-----
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
ShJ7Nyfd/u7m05tvbWULigwA6vNaLhTl
-----END PRIVATE KEY-----
""";

    // generated create-rsa-key-csr-crt-keystore.sh
    // signing cert sk-ca.localhost.crt must be in server sid trust store
    // mock service certificate
    public static final String TEST_CERT_PEM = "-----BEGIN CERTIFICATE-----"
        + """
        MIIEDjCCA5SgAwIBAgIUJZL9vyX+Muw9F5SQWAzwCmZD5xcwCgYIKoZIzj0EAwQw
        TDELMAkGA1UEBhMCRUUxEDAOBgNVBAcMB1RhbGxpbm4xETAPBgNVBAoMCHNrLWxv
        Y2FsMRgwFgYDVQQDDA9zay1jYS5sb2NhbGhvc3QwHhcNMjYwMzEzMTM1NDIxWhcN
        MzYwMzEwMTM1NDIxWjBjMQswCQYDVQQGEwJFRTEWMBQGA1UEAwwNVEVTVE5VTUJF
        UixPSzETMBEGA1UEBAwKVEVTVE5VTUJFUjELMAkGA1UEKgwCT0sxGjAYBgNVBAUT
        EVBOT0VFLTMwMzAzMDM5OTE0MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKC
        AgEAthy62fVSNGR3SpxKU9QKC4xG/UU2o7RbttvaB5WDyopI0X9dgMDyLKJj7Tou
        pY8ijdofhYXy+h2aN6snFoLTiAOAR+O4ZdmwtmHgCa1ASmcH3+SgnB0on/OPlUlJ
        06Nz0M684YmCSwd4v6KwgZfbxby1fC6KBCPIC9zw6+TMs7DI7P/LuRWh0qw6iMeJ
        z/4w1pZHn3bea0vOlX20fS9Ihv550iCoikY7ez2KXCYWPQp2xwPyLKAPpMbLcrjG
        kOTzwTJV1uDH4X07ipWdaSCDsq9vQTK9VVrjzI+UpehFAm8E2+XrSjDpOfuhHR1o
        V5fCoFq6chzj3JT+3B+tU4Ug5dbBr5XQc7vDuqW/BPBDsjYIcYXMCi4CbgwNtPBQ
        E1ttRjb8w29SqPcW9+njHShfLLquKIOm7hrc95IQ8BMngyOq3ukNZ1vYIONBc7Vk
        P7/SgKFzVVE/plY4xw5UdS1/WsUeV+LLWzCAJbzPQEPx0HeeyMyuhq8iPYBKYs5Z
        GcughZJfcHAgHez12HrZqRSrlDz/dIS10W6N9AZVLM20hO0eaXrk1b78V+9BRReh
        INDKMRjzERBFrd1jnVrUN6K2xg0WxHgYTq6CU3gd+kAhxJFjZytKCKVz73atfumH
        rSMLu3Z3i4+78M4IPP5WKcE5mdBbQS70fKJrlTghgkwNCG0CAwEAAaNyMHAwDgYD
        VR0PAQH/BAQDAgWgMBMGA1UdJQQMMAoGCCsGAQUFBwMCMAkGA1UdEwQCMAAwHQYD
        VR0OBBYEFCohbTYWHeMZKXrqO4lmbYprJA/oMB8GA1UdIwQYMBaAFGLfUN2tX/Fv
        Ai0qTyvpIeDIuQrSMAoGCCqGSM49BAMEA2gAMGUCMERa66+IGnwP7YCn0AeOaURk
        2szumhR1B+StUcEKgW2sn7oepXQIYQm7yOsCpHnvzQIxAOCRKeGG4Z7JRKS38VWf
        Es10/Ce0QrMiSbZjIFVnYmxwjTAQKI1Kpj/Oy2c1HgtRxw==
        """.replaceAll("\\s", "")
        + "-----END CERTIFICATE-----"; //remove all whitespace

    @SuppressWarnings("checkstyle:LineLength")
    static final String SESSION_TOKEN_WITH_FILTERED_DISCLOSURES_BASE64URL =
        "eyJraWQiOiJlYy1rZXktMjAyNiIsInR5cCI6InZuZC5jZG9jMi5zZXNzaW9uLXRva2VuLnYyK3NkLWp3dCIsImFsZyI6IkVTMjU2In0.eyJycENoYWxsZW5nZSI6InJrZ2s0cTE2eTYxbFJoRVJOcVZVdzBpdEhXZ3MzbWZMS3Y5cEQ2Z2xCdDl0ZDJRbmVhd1lLM0ZGOHFkMXBSakdDbnlyZ3NWTmprSXJ3T3NuZXkzOXl3PT0iLCJzdWIiOiJldHNpL1BOT0VFLTQwNTA0MDQwMDAxIiwic2lnbmF0dXJlIjp7InZhbHVlIjoiSlhtVmgwWlZqdFRUZHFFaHlET1NCejNMLyt4UHZPTGF1WXVmbmUydS8wRkVuZ0loQ0g4WEl6ZW5zazhsa3BLZlNxYVBjSzZReDF5bVZpdGM1YUNWY1N6bjRzUVV3SW5OODBXVEd1UTZtNTNESGdXWnFnS3NabHErSDcwamxxMXZSUE0vS3UwVEJIK01GRkhOeWp5SWZWN0MwOVMyK2pCbm1kYWEzM0FaNCtnOS9FNWVnL1p6QktHQWNnQmFyU01lYVpOdXNXQWNrdlJBQ3Y1WXlidUVYK0JSM3NYMXA0U09XNWdMT2lPOUwzaWtzVXNzVFp2K3MyVU9kbFVZTFpTVGVrWWIrYXFQRkdzODdjY1FQUmZuRlRWV1EwZ0pvVENWVG1lWWdXazA5MjZMOVREaUlQZlJ5cWxBbkNQNWRPZGlxRmNLSXFOZ3d4Z0RyZmlITmJ2R3hhQ3hGeVdJd2R2ekdmNEZPa3VMaVdndXVPQVo3YlgxVzIwQnNlZjFHTnRTQUdBVlRMbmJsMUNiOC8rT2dwRU52WXM2UUtxdXdiRlZHTDhzRDJEc1czNXZodllXdkJJN0tVK1NxODBnSDhURkFvdzNiN3QzOU80UEJmbVMzOUJrRTgxMW9mK2MwSFpNMXhJd3NTajZVVkR3bkNLOHYwZ1BKOGZMMmo5NDIrVUVVRUpQUEJaNmo1TWcrRmxBb1ZFR2pHakp1cDF3NVdCTFVBYTl1blRiNWp5UGtFUW84clVrS1Y0ZmQ0b21XTEpobGJXcmNYWlh5ZndrRGhJR211Uzh4Z2w2NSs0anMyVTI4THRDQzJYSjhlNWJhWm9rNWQ0Q2VVbFJvUjErbmIvZTg0cmthOUtPOUV6MGZHVmRlSmc0MFZhYTBWb2xBeDkxYVVmUS9mV0tpRXMxbHBOR0EyMG55cytJSTU3QWZqRDdkdGJxTzZaNzBXbEpUenMrREE0SHJEcXFQQ0ZRbVRmc2VhWFFOOVBxK3RnRFZqdFc1TlVRMTg4UlhSZ2pvalp4ZitCT2piVDJ6b0xxMS96VmdETkZTaW1kSElLQitJYUlwaDB5LzZHWWFGb0p2eERsRm9YSzhwUnU4My8vdGNnZmFuN1gzUWZKMnF1WTN3T2VDUHN5dmM1TklNdHRJdnhHRFlTdWI3QW9Ta3Z4eGFsdHg3Vy9FWEJWVmNDaWROZE1YM1ErZ2VNQlMzeS9NbEE3M0pqMXQxb1N6UmZxdFFpY2tYd0w3bmYrNXB5RzY2eUVFb1ZaZ1dVdE0zMUptSE9LVUF6UGVFOSthd2xRUjd3NXJSeitFemJ1MitLdlY2N1V4NFdXVG5JY0pOUG4rRkhOKzE1V1ZlTVhuRlhxVHZOSiIsInNlcnZlclJhbmRvbSI6InNWOXdsS3RaZTV0cjBnTjlpZXRQU0ovVCIsInVzZXJDaGFsbGVuZ2UiOiJmeWtaTHJmU2tsMW9uMXBITlBrZEFZUS1pekd0N1ZGeWN6eUNDM2x4cmlrIiwic2lnbmF0dXJlQWxnb3JpdGhtIjoicnNhc3NhLXBzcyIsImZsb3dUeXBlIjoiTm90aWZpY2F0aW9uIiwic2lnbmF0dXJlQWxnb3JpdGhtUGFyYW1ldGVycyI6eyJoYXNoQWxnb3JpdGhtIjoiU0hBLTI1NiIsIm1hc2tHZW5BbGdvcml0aG0iOnsiYWxnb3JpdGhtIjoiaWQtbWdmMSIsInBhcmFtZXRlcnMiOnsiaGFzaEFsZ29yaXRobSI6IlNIQS0yNTYifX0sInNhbHRMZW5ndGgiOjMyLCJ0cmFpbGVyRmllbGQiOiIweGJjIn19LCJpc3MiOiJodHRwczovL2Nkb2MyLWF1dGgtc2VydmVyLmVlIiwic2NoZW1lTmFtZSI6InNtYXJ0LWlkLWRlbW8iLCJzaWduYXR1cmVQcm90b2NvbCI6IlJTQVNTQS1QU1MrQUNTUF9WMiIsIl9zZCI6WyJuTEpHdS05X3lKMmlEOGhrRXU5Ym5yc0EzUHJ5Y3UwVVE1WXQ5UENTNV8wIl0sImludGVyYWN0aW9uc0RpZ2VzdCI6Im9sSk43T1hVdmZ5MWJVUE51NzEyWDNBN01PbTFCWGlXdGxBbXYrdWJJejA9IiwiX3NkX2FsZyI6InNoYS0yNTYiLCJleHAiOjE3NzY4NzI1MjksImlhdCI6MTc3Njc4NjEyOSwiaW50ZXJhY3Rpb25UeXBlVXNlZCI6ImNvbmZpcm1hdGlvbk1lc3NhZ2VBbmRWZXJpZmljYXRpb25Db2RlQ2hvaWNlIiwicnBOYW1lIjoiREVNTyJ9.5ORVwgy0tMpX5tdwZmhnnQK_H4ngB-duofWj2OYCrJU5kL5dUvJRSeiC5QLbuzH-8gk08b5asqIW9lWNEErAuw~WyJONGFScHVxNTVRZzh6LTVxS3dlRURBIiwiYXVkIixbeyIuLi4iOiIxM19rVmNGcXF3M0tycllRaUpnUEJ4Qm1zOG1rY0puMmtnNWZBRGc4aUlFIn0seyIuLi4iOiJkaG9VbVZod0c2TEJIbkwyMHJxbDZFTkVFdjlfdHBPOFo4aUVFbmVESmhjIn1dXQ~WyJMb2dqR24xc21ZNmxpWllEZnh4OGhnIiwiaHR0cDovL2xvY2FsaG9zdDo4MDgwL3Nlc3Npb25fbm9uY2VfMi9uclZjU0VjSHVXdDJTS2Zqa01tNlJRIl0~";
    @SuppressWarnings("checkstyle:LineLength")
    static final String SID_SIGNING_CERTIFICATE_BASE64URL =
        "MIIGpzCCBi6gAwIBAgIQGcJUbe6JHI6jJyV-42vjnTAKBggqhkjOPQQDAzBxMSwwKgYDVQQDDCNURVNUIG9mIFNLIElEIFNvbHV0aW9ucyBFSUQtUSAyMDI0RTEXMBUGA1UEYQwOTlRSRUUtMTA3NDcwMTMxGzAZBgNVBAoMElNLIElEIFNvbHV0aW9ucyBBUzELMAkGA1UEBhMCRUUwHhcNMjYwMTA2MTQyNTAxWhcNMjkwMTA1MTQyNTAwWjBXMQswCQYDVQQGEwJFRTEQMA4GA1UEAwwHVEVTVCxPSzENMAsGA1UEBAwEVEVTVDELMAkGA1UEKgwCT0sxGjAYBgNVBAUTEVBOT0VFLTQwNTA0MDQwMDAxMIIDIjANBgkqhkiG9w0BAQEFAAOCAw8AMIIDCgKCAwEAkI98VzyaeSueyaUQYIXMMf-1VY10Gw-b8Q13Rb9N62ROZY97wMIB__f8_PuOIoqkAPM6Tn_t4lp1R_rHrbuqs0hl2dgLlOcR5wmWmp7YfKPDvRndVLl_doIHruxY8O60rFGskSnqt4coHN4xGcmCyPkJoB8Rfm8-Y9poVKAreS0Ta32p5OSME0HjSs7-ahB2erWfb2GulFw1vyeH42d3XDpCCfd6CByvSsi4oByUqs5G-kjSrGUglflgWXK3MxBYto0swgsbD1nrW5doU_cMCfRoFURun4XguX8dTt9VeyqeJitxRfub2Hj18RbsKuoFNHQNOxAxRK4oTVCtUrYbVqBHDmoOm8r3CsSuqjuZ2njQybiUhBofpTVMCZ6lB6VgoLphmEwSEOQXIumpmpb2qJZqbZaBoyyWb4f5AQjw3Q5lwPSao5215hIgSuuENRezpP9rTzIwyOMbnV2nMSMInAuaXIXskB2NdpMsROsvOqBC0h5azTj9naCS-5EW-9eI7GGK03Du5JoKD5wYajJxfcxFwBAl8Ko71OvhGFtYiu-hqzz-CyG6NswB87KvzDYUCQ-0qOfgRBNCgYnbjnuYVJb3CGLp_cP5GmKtUC3wHX1WnPGyK4bD19Rcy-FhG6mD_ZrAPcmZ3s4FLLErpRJ3ui-fiMPLQl2bpCKTWoaEZoPg6Grnhr3bE2ZiKWmqdVwf30bG3-GnvTBTuF0T1lzt6NeBlB23SJsffCmzSFSNcFJHHYI1FYdZu2p0gL6KAabEmnE8GrTrCn93DFNBtoKu9vG30QrRzyh-itPvtn9w-9t-nDkhaVHmNCjWD1xcMeXsyK8ek0rbz5aVe_RPvCifhIpgjqNsDHh9q1QT9KIFsd6RD2XPMlekL9c6YiVY9H7uRyIQWqJwtrvNvBKj4ZT9745zTfkhCJTPvnLy-4iKeINVZ2f98BblsGAEHKGol8YA-3SRkPh9BVnVhSdI3lxCDEbmHuk21GIPE9689efSvbcDEHpqeYoxo3tXjl_hqfzPAgMBAAGjggH1MIIB8TAJBgNVHRMEAjAAMB8GA1UdIwQYMBaAFLAkFxmI42b4zShYZXtNFNiSZk9rMHAGCCsGAQUFBwEBBGQwYjAzBggrBgEFBQcwAoYnaHR0cDovL2Muc2suZWUvVEVTVF9FSUQtUV8yMDI0RS5kZXIuY3J0MCsGCCsGAQUFBzABhh9odHRwOi8vYWlhLmRlbW8uc2suZWUvZWlkcTIwMjRlMDAGA1UdEQQpMCekJTAjMSEwHwYDVQQDDBhQTk9FRS00MDUwNDA0MDAwMS1ERU0wLVEweAYDVR0gBHEwbzBjBgkrBgEEAc4fEQIwVjBUBggrBgEFBQcCARZIaHR0cHM6Ly93d3cuc2tpZHNvbHV0aW9ucy5ldS9yZXNvdXJjZXMvY2VydGlmaWNhdGlvbi1wcmFjdGljZS1zdGF0ZW1lbnQvMAgGBgQAj3oBAjAoBgNVHQkEITAfMB0GCCsGAQUFBwkBMREYDzE5MDUwNDA0MTIwMDAwWjAWBgNVHSUEDzANBgsrBgEEAYPmYgUHADA0BgNVHR8ELTArMCmgJ6AlhiNodHRwOi8vYy5zay5lZS90ZXN0X2VpZC1xXzIwMjRlLmNybDAdBgNVHQ4EFgQUX9YaVGlPdUOO2J6rzNc4sljBQBAwDgYDVR0PAQH_BAQDAgeAMAoGCCqGSM49BAMDA2cAMGQCMHhYJCeKceJv_m0xcFRssS4WVFnnCryDiuSEpjDZu0irJ_XurXXIFDr-9hhl2x7GMwIwbiD5GALRtwzUaEh-SV9jigT9Oc336f6QYf8YaSA0-Un8eRQPa9wTK0cSQrM_CUIu";
    static final String SESSION_NONCE_FOR_TOKEN = "nrVcSEcHuWt2SKfjkMm6RQ";

    // identifier from above SID_SIGNING_CERTIFICATE_BASE64URL
    // and SESSION_TOKEN_WITH_FILTERED_DISCLOSURES_BASE64URL
    public static final String TEST_IDENTIFIER = "40504040001";
    public static final String TEST_ETSI_RECIPIENT = "etsi/PNOEE-" + TEST_IDENTIFIER;

    private TestData() {
        // utility class
    }

    @SneakyThrows
    public static Path getKeysDirectory() {
        Properties prop = new Properties();
        //generated during maven generate-test-resources phase, see pom.xml
        String windowsPathEscape = new String(TestData.class.getClassLoader()
            .getResourceAsStream("test.properties").readAllBytes());
        prop.load(new StringReader(windowsPathEscape.replace("\\", "\\\\")));
        String keysProperty = prop.getProperty("cdoc2.keys.dir");
        log.debug("Value for property cdoc2.keys.dir is {}", keysProperty);
        Path keysPath = Path.of(keysProperty).normalize();
        log.debug("Loading keys/certs from {}", keysPath);
        return keysPath;
    }

    @SneakyThrows
    public static KeyStore loadKeyStore(String keyStoreType, Path keyStoreFile, String keyStorePassword) {
        log.debug("loadKeyStore({}, {})", keyStoreType, keyStoreFile);
        try {
            var keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(Files.newInputStream(keyStoreFile), keyStorePassword.toCharArray());

            keyStore.aliases().asIterator().forEachRemaining(a -> log.debug("Alias in keystore: {}", a));
            return keyStore;
        } catch (GeneralSecurityException | IOException e) {
            log.error("Error initializing key stores", e);
            throw e;
        }
    }

    /**
     * Generate Auth ticket with TestData.TEST_RSAKEY.
     * @param eid example 40504040001
     * @param serverUrl
     * @param shareId
     * @param nonce
     * @return
     */
    @SneakyThrows
    public static String generateTestAuthTicket(String eid, String serverUrl, String shareId, String nonce) {

        X509Certificate cert = X509CertUtils.parseWithException(TEST_CERT_PEM);
        String testSemanticsIdentifier = SIDCertificateUtil.getSemanticsIdentifier(cert); //PNOEE-40504040001
        EtsiIdentifier etsi = new EtsiIdentifier("etsi/" + testSemanticsIdentifier);

        // Only have certificate and RSA private key for single
        assertTrue(testSemanticsIdentifier.contains(eid),
            "Only " + testSemanticsIdentifier + " is supported for auth ticket generation");


        JWK jwk = JWK.parseFromPEMEncodedObjects(TEST_RSAKEY);
        RSAKey privateKey = jwk.toRSAKey();

        JWSSigner jwsSigner = new RSASSASigner(privateKey) {
            // Smart-ID JWSSigner supports only RS256
            @Override
            public Set<JWSAlgorithm> supportedJWSAlgorithms() {
                return Set.of(JWSAlgorithm.RS256);
            }
        };

        AuthTokenCreator token = AuthTokenCreator.builder()
            .withEtsiIdentifier(etsi) // "iss" field etsi/PNOEE-40504040001
            .withShareAccessData(new ShareAccessData(
                serverUrl,
                shareId,
                nonce))
            .build();


        token.sign(jwsSigner);

        return token.createTicketForShareId(shareId);
    }

}
