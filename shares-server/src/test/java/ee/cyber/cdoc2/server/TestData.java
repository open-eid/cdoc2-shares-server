package ee.cyber.cdoc2.server;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Properties;

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
-----BEGIN RSA PRIVATE KEY-----
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
-----END RSA PRIVATE KEY-----
""";

    // generated create-rsa-key-csr-crt-keystore.sh
    // signing cert sk-ca.localhost.crt must be in server sid trust store
    // mock service certificate
    public static final String TEST_CERT_PEM = "-----BEGIN CERTIFICATE-----"
        + """
        MIIDuTCCA2CgAwIBAgIUTL1AousETAVENwEl62mocPaUfZQwCgYIKoZIzj0EAwQw
        TDELMAkGA1UEBhMCRUUxEDAOBgNVBAcMB1RhbGxpbm4xETAPBgNVBAoMCHNrLWxv
        Y2FsMRgwFgYDVQQDDA9zay1jYS5sb2NhbGhvc3QwHhcNMjQxMTI1MjAwMDQ5WhcN
        MjUxMTI1MjAwMDQ5WjBjMRowGAYDVQQFExFQTk9FRS0zMDMwMzAzOTkxNDELMAkG
        A1UEKgwCT0sxEzARBgNVBAQMClRFU1ROVU1CRVIxFjAUBgNVBAMMDVRFU1ROVU1C
        RVIsT0sxCzAJBgNVBAYTAkVFMIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKC
        AgEA1TyEtd/tUQsDtNfwy7l34SoUvBAnAXU8CFnXtF1e0+2Rb1hHEm/mRiwHvjmd
        VH6Gud03RVWi7Xc+pJLddM/EUxKdF5Rpe9exzNN7yTiOOIP3WcLSjwMdEOOUBE1a
        ysUePcqQBB1Se/c2yiQuOBOe5OMzCZbvrv8JRW1T+FGmDVAtHTS04Yv6gnudAd/B
        pCdbDdCzdaV2BgI5WME+IXnh7Nwg3GQuqNTwsZWNbfG+1gATLrfBaPWE5alQ1s2K
        c+dbURJMA8JraKpVYx1P2h4jyCAQOe2Lza8d+/HPez2BiYFPJVO2ogLxLovcdfmh
        ztTm2xDZBTzY0/c2XbjMpMERgIFIssH188FS0TXkWmwBG+cVhhl5SPY6YUEaRDa6
        lRe4940NGgZovlVcc0uLb+LVMcA9S6EE4egv50b+hFXQaejfuhnw5cnoCfvzb1dn
        Y8dY7zVHMIDUyt0aAoU6OHRlSsBYTdaHYQuW0UxqzXZBzoGG6wZ6Wkycytg4DIJJ
        yoI0Ces8MYHX7Kek8kcX0GsXGiO5HnmHGFkRgPwTOOhrPhcLalLOvFL+JtUotaot
        8wlay2JybaHbYtebjio6PkAGHiDOxMaU+8R25S7PhWPr8A9a+rftNXDQDZOSjZxF
        TtLjcXsXesZ7GU63arw073K7Kp3NoQ2r9oemq0ZawmDf9vMCAwEAAaM+MDwwHwYD
        VR0jBBgwFoAUbs3btcBBYBn+RwvDkSG9Gz2Sxl8wDAYDVR0TAQH/BAIwADALBgNV
        HQ8EBAMCBaAwCgYIKoZIzj0EAwQDRwAwRAIgJsR3WD6ZAIS5+K3YZ822QjmZYHOT
        oeW6Qz1MZFgQba8CIBCrja2kNYPtyJmJF/sespAVdz7eYHxgNUkM4cqEWFkz
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
//        RSAKey rsaPublicJWK = new RSAKey.Builder(privateKey.toRSAPublicKey())
//            .keyID(etsi.toString())
//            .build();

        JWSSigner jwsSigner = new RSASSASigner(privateKey);

        AuthTokenCreator token = AuthTokenCreator.builder()
            .withEtsiIdentifier(etsi) // "iss" field etsi/PNOEE-30303039914
            .withShareAccessData(new ShareAccessData(
                serverUrl,
                shareId,
                nonce))
//            .withShareAccessData(new ShareAccessData(
//                "https://cdoc-ccs.smit.ee:443/key-shares/",
//                "5BAE4603-C33C-4425-B301-125F2ACF9B1E",
//                "9d23660840b427f405009d970d269770417bc769"))
            .build();

        
        token.sign(jwsSigner, testSemanticsIdentifier); // header "kid" PNOEE-30303039914

        return token.createTicketForShareId(shareId);
    }

}
