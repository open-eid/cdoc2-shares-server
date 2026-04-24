package ee.cyber.cdoc2.server.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.security.SecureRandom;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import ee.cyber.cdoc2.server.generated.api.SessionNonceApiDelegate;
import ee.cyber.cdoc2.server.generated.model.NonceResponse;
import ee.cyber.cdoc2.server.model.entity.SessionNonceDb;
import ee.cyber.cdoc2.server.model.repository.SessionNonceRepository;

import static ee.cyber.cdoc2.server.Utils.base64UrlEnc;
import static ee.cyber.cdoc2.server.Utils.createNonceResponse;


/**
 * Implements API for session nonce
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SessionNonceApiService implements SessionNonceApiDelegate {

    private static final int SESSION_NONCE_BYTES = 16;
    private final SessionNonceRepository sessionNonceRepository;

    @Override
    public ResponseEntity<NonceResponse> generateSessionNonce(Object body) {
        log.info("Generating session nonce");

        var sessionNonce = new SessionNonceDb();
        sessionNonce.setNonce(generateSessionNonce());

        var nonce = sessionNonceRepository.save(sessionNonce);

        log.info("SessionNonce(nonce = {}) created", base64UrlEnc(nonce.getNonce()));

        return ResponseEntity.ok(createNonceResponse(nonce.getNonce()));
    }

    private static byte[] generateSessionNonce() {
        byte[] nonce = new byte[SESSION_NONCE_BYTES];
        SecureRandom random = new SecureRandom();
        random.nextBytes(nonce);
        return nonce;
    }
}
