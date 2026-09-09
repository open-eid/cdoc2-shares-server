package ee.cyber.cdoc2.server.model;

import java.nio.charset.StandardCharsets;
import java.security.DrbgParameters;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;


public final class Crypto {

    private static final Logger log = LoggerFactory.getLogger(Crypto.class);

    private static final Object LOCK = new Object();

    private static SecureRandom secureRandomInstance = null;

    private Crypto() { }

    @SuppressFBWarnings(value = "MS_EXPOSE_REP",
        justification = "SecureRandom is thread-safe and intended to be shared as a single instance")
    public static SecureRandom getSecureRandom() throws NoSuchAlgorithmException {
        synchronized (LOCK) {
            if (secureRandomInstance == null) {
                secureRandomInstance = createSecureRandom();
            }

            return secureRandomInstance;
        }
    }

    private static SecureRandom createSecureRandom() throws NoSuchAlgorithmException {
        log.debug("Initializing SecureRandom");
        SecureRandom sRnd = SecureRandom.getInstance("DRBG",
            DrbgParameters.instantiation(256, DrbgParameters.Capability.PR_AND_RESEED,
                "CDOC2".getBytes(StandardCharsets.UTF_8)));
        log.info("Initialized SecureRandom.");
        return sRnd;
    }

}
