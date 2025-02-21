package ee.cyber.cdoc2.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;


/**
 * Smart ID/Mobile ID certificates configuration properties
 */
@ConfigurationProperties(prefix = "cdoc2.auth-x5c")
public record AuthCertificateConfigProperties(
    boolean revocationChecksEnabled,
    boolean signCertForbidden
) {

    public AuthCertificateConfigProperties() {
        this(false, true);
    }
}
