package ee.cyber.cdoc2.server.config;

import lombok.RequiredArgsConstructor;

import java.security.KeyStore;

import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Configuration;


@Configuration
@RequiredArgsConstructor
public class SidTrustedIssuers {
    private static final String SID_TRUSTED_ISSUERS_BUNDLE_NAME = "sid-trusted-issuers";

    private final SslBundles sslBundles;

    public KeyStore getTrustStore() {
        return sslBundles.getBundle(SID_TRUSTED_ISSUERS_BUNDLE_NAME).getStores().getTrustStore();
    }
}
