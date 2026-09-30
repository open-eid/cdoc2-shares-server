package ee.cyber.cdoc2.server.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.text.ParseException;
import java.util.Collections;
import java.util.List;

import org.springframework.context.annotation.Configuration;

import com.nimbusds.jose.jwk.JWK;

import ee.cyber.cdoc2.server.clients.AuthServerClient;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class AuthServerJwkConf {
    private volatile List<JWK> publicKeys;
    private final AuthServerClient authServerClient;

    public List<JWK> getPublicKeys() {
        List<JWK> keys = this.publicKeys;
        if (keys == null) {
            synchronized (this) {
                keys = this.publicKeys;
                if (keys == null) {
                    try {
                        keys = List.copyOf(authServerClient.getAuthServerWellKnown());
                        this.publicKeys = keys;
                    } catch (ParseException e) {
                        log.error("Failed to parse Auth server well-known JWK set", e);
                        throw new IllegalStateException("Invalid JWK set from auth server", e);
                    }
                }
            }
        }
        return Collections.unmodifiableList(keys);
    }
}
