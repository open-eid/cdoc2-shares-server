package ee.cyber.cdoc2.server.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.text.ParseException;
import java.util.List;

import org.springframework.context.annotation.Configuration;

import com.nimbusds.jose.jwk.JWK;

import ee.cyber.cdoc2.server.clients.RpServerClient;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class RpServerJwkConf {
    private List<JWK> publicKeys;
    private final RpServerClient rpServerClient;

    public List<JWK> getPublicKeys() {
        if (this.publicKeys == null) {
            try {
                this.publicKeys = rpServerClient.getRpServerWellKnown();
            } catch (ParseException e) {
                log.error("Failed to parse RP server well-known JWK set", e);
                throw new RuntimeException(e);
            }
        }

        return this.publicKeys;
    }
}
