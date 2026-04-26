package ee.cyber.cdoc2.server.config;


import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cdoc2.rp-server")
public record RpServerConfigProperties(String rpName, String schemeName) {

    public RpServerConfigProperties() {
        this("DEMO", "smart-id-demo");
    }
}
