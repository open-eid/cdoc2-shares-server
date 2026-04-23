package ee.cyber.cdoc2.server.clients;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class AuthServerRestClientConfiguration {
    private static final String CONF_DEFAULT_READ_TIMEOUT = "5000";
    private static final String CONF_DEFAULT_CONNECTION_REQUEST_TIMEOUT = "5000";

    @ConfigurationProperties(prefix = "app.restclient.auth-server")
    public record AppProperties(
        String hostUri,
        @DefaultValue(CONF_DEFAULT_READ_TIMEOUT) int readTimeout,
        @DefaultValue(CONF_DEFAULT_CONNECTION_REQUEST_TIMEOUT) int connectionRequestTimeout
    ) {
    }

    @Bean
    public RestClient authServerRestClient(AppProperties props) {
        HttpComponentsClientHttpRequestFactory factory =
            new HttpComponentsClientHttpRequestFactory();
        factory.setReadTimeout(props.readTimeout);
        factory.setConnectionRequestTimeout(props.connectionRequestTimeout);

        return RestClient.builder()
            .baseUrl(props.hostUri)
            .requestFactory(factory)
            .build();
    }
}

