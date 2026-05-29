package ee.cyber.cdoc2.server;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import java.security.KeyStore;
import java.util.Map;

import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


class InfoApiServiceTest extends BaseInitializationTest {

    @Test
    @SuppressWarnings("unchecked")
    void infoEndpointIsPublicAndReturnsExpectedFields() throws Exception {
        RestTemplate restTemplate = buildTlsRestTemplate(CLIENT_TRUST_STORE);

        ResponseEntity<Map> response = restTemplate.getForEntity(baseUrl + "/info", Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertNotNull(body.get("build"));
    }

    private static RestTemplate buildTlsRestTemplate(KeyStore trustStore) throws Exception {
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), null);

        var connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
            .setSSLSocketFactory(new SSLConnectionSocketFactory(sslContext))
            .build();

        var httpClient = HttpClients.custom()
            .setConnectionManager(connectionManager)
            .build();

        return new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));
    }

}
