package ee.cyber.cdoc2.server;

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
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import static org.junit.Assert.assertTrue;


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

      //identifier from above TEST_CERT
    public static final String TEST_IDENTIFIER = "30303039914";
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
     * @param eid example 30303039914
     * @param serverUrl
     * @param shareId
     * @param nonce
     * @return
     */
    @SneakyThrows
    public static String generateTestAuthTicket(String eid, String serverUrl, String shareId, String nonce) {

        X509Certificate cert = X509CertUtils.parseWithException(TEST_CERT_PEM);
        String testSemanticsIdentifier = SIDCertificateUtil.getSemanticsIdentifier(cert); //PNOEE-30303039914
        EtsiIdentifier etsi = new EtsiIdentifier("etsi/" + testSemanticsIdentifier);
        
        // Only have certificate and RSA private key for single
        assertTrue("Only " + testSemanticsIdentifier + " is supported for auth ticket generation",
            testSemanticsIdentifier.contains(eid));
        

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
            .withEtsiIdentifier(etsi) // "iss" field etsi/PNOEE-30303039914
            .withShareAccessData(new ShareAccessData(
                serverUrl,
                shareId,
                nonce))
            .build();

        
        token.sign(jwsSigner);

        return token.createTicketForShareId(shareId);
    }

}
