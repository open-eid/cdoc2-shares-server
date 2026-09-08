package ee.cyber.cdoc2.server.clients;

import java.text.ParseException;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;


@Component
public class RpServerClient {
    private static final String WELL_KNOWN_PATH = ".well-known/jwks.jws";
    private final RestClient restClient;

    public RpServerClient(@Qualifier("rpServerRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public List<JWK> getRpServerWellKnown() throws ParseException {
        String jwkJson = restClient.get()
            .uri(WELL_KNOWN_PATH)
            .retrieve()
            .body(String.class);

        return JWKSet.parse(jwkJson).getKeys();
    }
}
