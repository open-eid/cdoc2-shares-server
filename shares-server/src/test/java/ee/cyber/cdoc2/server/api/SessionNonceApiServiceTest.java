package ee.cyber.cdoc2.server.api;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ee.cyber.cdoc2.server.SessionNonceIntegrationTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class SessionNonceApiServiceTest extends SessionNonceIntegrationTest {

    private SessionNonceApiService sessionNonceApiService;

    @BeforeEach
    public void setUp() {
        sessionNonceApiService = new SessionNonceApiService();
    }

    @Test
    void shouldGetSessionNonce() {
        var resp = sessionNonceApiService.generateSessionNonce((Object) null);

        assertTrue(resp.getStatusCode().is2xxSuccessful());
        assertNotNull(resp.getBody());
        assertNotNull(resp.getBody().getNonce());
        // TODO: once the nonce is saved to database, check that it is present and it matches
    }
}
