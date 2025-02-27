package ee.cyber.cdoc2.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;


/**
 * Key Shares nonce configuration properties
 */
@ConfigurationProperties(prefix = "cdoc2.nonce")
public record NonceConfigProperties(long expirationSeconds) {
    public NonceConfigProperties() {
        this(300L);
    }
}
