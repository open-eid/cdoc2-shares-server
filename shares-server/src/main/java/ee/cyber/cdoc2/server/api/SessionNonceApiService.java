package ee.cyber.cdoc2.server.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.security.SecureRandom;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import ee.cyber.cdoc2.server.generated.api.SessionNonceApiDelegate;
import ee.cyber.cdoc2.server.generated.model.NonceResponse;

import static ee.cyber.cdoc2.server.Utils.base64UrlEnc;
import static ee.cyber.cdoc2.server.Utils.createNonceResponse;


/**
 * Implements API for session nonce
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SessionNonceApiService implements SessionNonceApiDelegate {

    @Override
    public ResponseEntity<NonceResponse> generateSessionNonce(Object body) {
        log.info("Generating session nonce");

        var nonce = generateDummyNonce();

        log.info("SessionNonce(nonce = {}) created", base64UrlEnc(nonce));

        return ResponseEntity.ok(createNonceResponse(nonce));
    }

    private static byte[] generateDummyNonce() {
        byte[] nonce = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(nonce);
        return nonce;
    }
}
