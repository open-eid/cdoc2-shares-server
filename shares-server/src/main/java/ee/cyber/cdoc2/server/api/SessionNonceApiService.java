package ee.cyber.cdoc2.server.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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

    private final SessionNonceRepository sessionNonceRepository;

    @Override
    public ResponseEntity<NonceResponse> generateSessionNonce(Object body) {
        log.info("Generating session nonce");

        var nonce = sessionNonceRepository.save(
            new SessionNonceDb()
        );

        log.info("SessionNonce(nonce = {}) created", base64UrlEnc(nonce.getNonce()));

        return ResponseEntity.ok(createNonceResponse(nonce.getNonce()));
    }
}
