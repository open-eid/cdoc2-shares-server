package ee.cyber.cdoc2.server;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;

import ee.cyber.cdoc2.server.generated.model.NonceResponse;


/**
 * Server utilities.
 */
public final class Utils {

    private Utils() {
    }

    public static URI getPathAndQueryPart(URI fullURI) throws URISyntaxException {
        // return only path and query part of URI as host and port might be different, when running behind load balancer

        String uriStr = fullURI.toString();
        URI uri = new URI(uriStr).normalize();

        if (uri.getQuery() != null) {
            return new URI(uri.getPath() + '?' + uri.getQuery());
        } else {
            return new URI(uri.getPath());
        }
    }

    public static String base64UrlEnc(byte[] src) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(src);
    }

    public static NonceResponse createNonceResponse(byte[] nonce) {
        var response = new NonceResponse();
        response.setNonce(base64UrlEnc(nonce));

        return response;
    }
}
