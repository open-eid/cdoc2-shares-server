package ee.cyber.cdoc2.server;

import ee.cyber.cdoc2.server.config.MonitoringConfigProperties;
import ee.cyber.cdoc2.server.config.SecurityConfiguration;


/**
 * Key shares server security configuration
 */
public class KeySharesServerSecurityConfiguration extends SecurityConfiguration {

    public KeySharesServerSecurityConfiguration(MonitoringConfigProperties configProperties) {
        super(configProperties);
    }

}
