package ee.cyber.cdoc2.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;


/**
 * Key Shares server security configuration
 */
public class KeySharesServerSecurityConfiguration extends SecurityConfiguration {

    private final KeySharesServerConfigProperties serverConfigProperties;

    public KeySharesServerSecurityConfiguration(
        ConfigProperties configProperties,
        KeySharesServerConfigProperties serverConfigProperties
    ) {
        super(configProperties);
        this.serverConfigProperties = serverConfigProperties;
    }

    @Override
    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails user = User.builder()
            .username(serverConfigProperties.username())
            .password("{noop}" + serverConfigProperties.password())
            .roles("USER")
            .build();
        return new InMemoryUserDetailsManager(user);
    }

}
