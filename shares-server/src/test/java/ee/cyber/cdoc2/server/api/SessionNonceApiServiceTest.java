package ee.cyber.cdoc2.server.api;


import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ee.cyber.cdoc2.server.SessionNonceIntegrationTest;
import ee.cyber.cdoc2.server.model.repository.SessionNonceRepository;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class SessionNonceApiServiceTest extends SessionNonceIntegrationTest {

    @Autowired
    private SessionNonceRepository sessionNonceRepository;

    private SessionNonceApiService sessionNonceApiService;

    private static final int EXPECTED_SESSION_NONCE_BYTES = 16;

    @BeforeEach
    public void setUp() {
        sessionNonceApiService = new SessionNonceApiService(sessionNonceRepository);
    }

    @Test
    void shouldGetSessionNonce() {
        var resp = sessionNonceApiService.generateSessionNonce((Object) null);

        assertTrue(resp.getStatusCode().is2xxSuccessful());
        assertNotNull(resp.getBody());
        assertNotNull(resp.getBody().getNonce());

        var sessionNonce = resp.getBody().getNonce();
        assertTrue(sessionNonce.length() >= EXPECTED_SESSION_NONCE_BYTES);

        byte[] decodedNonce = Base64.getUrlDecoder().decode(sessionNonce);
        var nonceFromDb = sessionNonceRepository.findByNonce(decodedNonce);
         assertTrue(nonceFromDb.isPresent());
    }
}
