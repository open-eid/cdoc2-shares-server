package ee.cyber.cdoc2.server;

import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.util.Base64;
import java.util.List;

import org.springframework.stereotype.Component;

import com.nimbusds.jose.jwk.JWK;

import ee.cyber.cdoc2.auth.SessionTokenVerifier;
import ee.cyber.cdoc2.auth.TokenVerificationResponse;
import ee.cyber.cdoc2.auth.exception.VerificationException;
import ee.cyber.cdoc2.server.config.AuthCertificateConfigProperties;
import ee.cyber.cdoc2.server.config.AuthServerJwkConf;
import ee.cyber.cdoc2.server.config.SidTrustedIssuers;
import ee.cyber.cdoc2.server.model.repository.SessionNonceRepository;


@Component
@RequiredArgsConstructor
public class ValidateSessionToken {

    private final AuthServerJwkConf authServerJwkConf;
    private final SidTrustedIssuers sidTrustedIssuers;
    private final AuthCertificateConfigProperties certificateConfig;
    private final SessionNonceRepository sessionNonceRepository;
    private final Clock clock;

    public TokenVerificationResponse execute(
        String sessionToken,
        String signingCertificate
    ) throws VerificationException {
        List<JWK> keys = authServerJwkConf.getPublicKeys();

        SessionTokenVerifier sessionTokenVerifier = new SessionTokenVerifier(
            sidTrustedIssuers.getTrustStore(),
            certificateConfig.revocationChecksEnabled(),
            clock
        );

        TokenVerificationResponse response = sessionTokenVerifier.verify(
            sessionToken,
            signingCertificate,
            keys
        );

        String uriString = response.nonceUri().toString();
        String sessionNonce = uriString.substring(uriString.lastIndexOf('/') + 1);
        var decodedNonce = Base64.getUrlDecoder().decode(sessionNonce);

        if (!sessionNonceRepository.existsByNonce(decodedNonce)) {
            throw new VerificationException("Could not find session nonce");
        }

        return response;
    }
}
