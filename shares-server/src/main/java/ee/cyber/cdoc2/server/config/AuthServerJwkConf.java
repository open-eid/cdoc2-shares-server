package ee.cyber.cdoc2.server.config;

import lombok.RequiredArgsConstructor;

import java.text.ParseException;
import java.util.List;

import org.springframework.context.annotation.Configuration;

import com.nimbusds.jose.jwk.JWK;

import ee.cyber.cdoc2.server.clients.AuthServerClient;

@Configuration
@RequiredArgsConstructor
public class AuthServerJwkConf {
    private List<JWK> publicKeys;
    private final AuthServerClient authServerClient;

    public List<JWK> getPublicKeys() {
        if (this.publicKeys == null) {
            try {
                this.publicKeys = authServerClient.getAuthServerWellKnown();
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }

        return this.publicKeys;
    }
}
