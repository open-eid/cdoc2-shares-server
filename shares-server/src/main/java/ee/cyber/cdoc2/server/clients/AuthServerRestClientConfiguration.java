package ee.cyber.cdoc2.server.clients;

import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import javax.net.ssl.SSLContext;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.http.ssl.TLS;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.Timeout;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class AuthServerRestClientConfiguration {
    private static final String CONF_DEFAULT_READ_TIMEOUT = "5000";
    private static final String CONF_DEFAULT_CONNECTION_REQUEST_TIMEOUT = "5000";
    private static final String SSL_BUNDLE_NAME = "trusted-infra";

    @ConfigurationProperties(prefix = "app.restclient.auth-server")
    public record AppProperties(
        String hostUri,
        @DefaultValue(CONF_DEFAULT_READ_TIMEOUT) int readTimeout,
        @DefaultValue(CONF_DEFAULT_CONNECTION_REQUEST_TIMEOUT) int connectionRequestTimeout
    ) {
    }

    @Bean
    public RestClient authServerRestClient(AppProperties props, SslBundles sslBundles)
        throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        KeyStore trustStore = sslBundles.getBundle(SSL_BUNDLE_NAME).getStores().getTrustStore();

        SSLContext sslContext = SSLContexts.custom()
            .loadTrustMaterial(trustStore, null)
            .build();

        PoolingHttpClientConnectionManager connectionManager =
            PoolingHttpClientConnectionManagerBuilder.create()
                .setTlsSocketStrategy(
                    ClientTlsStrategyBuilder.create()
                        .setSslContext(sslContext)
                        .setTlsVersions(TLS.V_1_3)
                        .buildClassic()
                )
                .setDefaultSocketConfig(
                    SocketConfig.custom()
                        .setSoTimeout(Timeout.ofMilliseconds(props.readTimeout()))
                        .build()
                )
                .setDefaultConnectionConfig(
                    ConnectionConfig.custom()
                        .setConnectTimeout(Timeout.ofMilliseconds(props.connectionRequestTimeout()))
                        .build()
                )
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
            .setConnectionManager(connectionManager)
            .build();

        return RestClient.builder()
            .baseUrl(props.hostUri)
            .requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient))
            .build();
    }
}

