package ee.cyber.cdoc2.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;


/**
 * Security configuration properties
 */
@ConfigurationProperties(prefix = "open-api.client.key-shares")
public record KeySharesServerConfigProperties(String username, String password) {
}
