package ee.cyber.cdoc2.server.config;

import lombok.RequiredArgsConstructor;

import java.text.ParseException;
import java.util.List;

import org.springframework.context.annotation.Configuration;

import com.nimbusds.jose.jwk.JWK;

import ee.cyber.cdoc2.server.clients.RpServerClient;

@Configuration
@RequiredArgsConstructor
public class RpServerJwkConf {
    private List<JWK> publicKeys;
    private final RpServerClient rpServerClient;

    public List<JWK> getPublicKeys() {
        if (this.publicKeys == null) {
            try {
                this.publicKeys = rpServerClient.getRpServerWellKnown();
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }

        return this.publicKeys;
    }
}
