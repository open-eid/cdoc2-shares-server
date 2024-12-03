package ee.cyber.cdoc2.server.model;

import java.security.DrbgParameters;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class Crypto {

    private static final Logger log = LoggerFactory.getLogger(Crypto.class);

    private static SecureRandom secureRandomInstance = null;

    private Crypto() { }

    public static synchronized SecureRandom getSecureRandom() throws NoSuchAlgorithmException {
        if (secureRandomInstance == null) {
            secureRandomInstance = createSecureRandom();
        }

        return secureRandomInstance;
    }

    private static SecureRandom createSecureRandom() throws NoSuchAlgorithmException {
        log.debug("Initializing SecureRandom");
        SecureRandom sRnd = SecureRandom.getInstance("DRBG",
            DrbgParameters.instantiation(256, DrbgParameters.Capability.PR_AND_RESEED, "CDOC2".getBytes()));
        log.info("Initialized SecureRandom.");
        return sRnd;
    }

}
