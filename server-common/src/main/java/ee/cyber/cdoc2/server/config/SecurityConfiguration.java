package ee.cyber.cdoc2.server.config;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;


/**
 * Security configuration
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@SuppressWarnings("java:S4502")
public class SecurityConfiguration {

    private final ConfigProperties credentials;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(authorize ->
                authorize
                    .requestMatchers(new AntPathRequestMatcher("/key-shares**")).permitAll()
                    // authenticated URI must go first
                    .requestMatchers(new AntPathRequestMatcher("/actuator/prometheus")).authenticated()
                    .requestMatchers(new AntPathRequestMatcher("/actuator/**")).permitAll()
                    .anyRequest().authenticated()
            )
            .x509(x509 ->
                x509
                    .subjectPrincipalRegex("CN=(.*?)(?:,|$)")
                    .userDetailsService(userDetailsService())
            )
            .sessionManagement(sessionManagementConfigurer ->
                sessionManagementConfigurer.sessionCreationPolicy(SessionCreationPolicy.NEVER)
            )
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails user = User.builder()
            .username(credentials.username())
            .password("{noop}" + credentials.password())
            .roles("USER")
            .build();
        return new InMemoryUserDetailsManager(user);
    }

}
