package ee.cyber.cdoc2.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;


/**
 * Configuration properties for key share. {@code Duration#parse} format is implemented for
 * extracting duration dates.
 *
 * @param defaultExpirationDuration default value for share expiration duration
 * @param maxExpirationDuration max allowed value for share expiration duration
 */
@ConfigurationProperties(prefix = "key-share")
public record KeyShareExpiryConfigProperties(
    String defaultExpirationDuration,
    String maxExpirationDuration
) {

    public KeyShareExpiryConfigProperties() {
        this("P1095D", "P1825D");
    }
}
