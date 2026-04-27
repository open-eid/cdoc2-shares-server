package ee.cyber.cdoc2.server.api;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;

import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.x509.Certificate;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509CertificateHolder;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.server.ResponseStatusException;

import com.nimbusds.jose.util.X509CertUtils;

import ee.cyber.cdoc2.auth.AuthTokenVerifier;
import ee.cyber.cdoc2.auth.ShareAccessData;
import ee.cyber.cdoc2.auth.TokenVerificationResponse;
import ee.cyber.cdoc2.auth.exception.VerificationException;
import ee.cyber.cdoc2.server.ValidateSessionToken;
import ee.cyber.cdoc2.server.config.AuthCertificateConfigProperties;
import ee.cyber.cdoc2.server.config.NonceConfigProperties;
import ee.cyber.cdoc2.server.config.RpServerConfigProperties;
import ee.cyber.cdoc2.server.generated.api.KeySharesApi;
import ee.cyber.cdoc2.server.generated.api.KeySharesApiController;
import ee.cyber.cdoc2.server.generated.api.KeySharesApiDelegate;
import ee.cyber.cdoc2.server.generated.model.KeyShare;
import ee.cyber.cdoc2.server.generated.model.NonceResponse;
import ee.cyber.cdoc2.server.model.entity.KeyShareDb;
import ee.cyber.cdoc2.server.model.entity.KeyShareNonceDb;
import ee.cyber.cdoc2.server.model.repository.KeyShareNonceRepository;
import ee.cyber.cdoc2.server.model.repository.KeyShareRepository;

import static ee.cyber.cdoc2.server.Utils.createNonceResponse;
import static ee.cyber.cdoc2.server.Utils.getPathAndQueryPart;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;


/**
 * Implements API for getting and creating CDOC2 key shares {@link KeySharesApi}
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class KeyShareApiService implements KeySharesApiDelegate {
    private final AuthCertificateConfigProperties certificateConfig;
    private final RpServerConfigProperties rpServerConfigProperties;
    private final NonceConfigProperties nonceConfigProperties;
    private final NativeWebRequest nativeWebRequest;
    private final KeyShareRepository keyShareRepository;
    private final KeyShareNonceRepository shareNonceRepository;
    private final ValidateSessionToken validateSessionToken;

    // configure sslBundles in application.properties
    // https://docs.spring.io/spring-boot/reference/features/ssl.html#features.ssl.pem
    private final SslBundles sslBundles;

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return Optional.of(this.nativeWebRequest);
    }

    @Override
    public ResponseEntity<Void> createKeyShare(KeyShare keyShare) {
        log.trace("createKeyShare(share={} bytes, recipient={} bytes)",
            keyShare.getShare().length, keyShare.getRecipient()
        );

        try {
            var saved = this.keyShareRepository.save(
                new KeyShareDb()
                    .setShare(keyShare.getShare())
                    .setRecipient(keyShare.getRecipient())
            );

            log.info("KeyShare(shareId={}) created", saved.getShareId());

            URI created = getResourceLocation(saved.getShareId());

            return ResponseEntity.created(created).build();
        } catch (Exception e) {
            log.error(
                "Failed to save key share(share={} bytes, recipient={})",
                keyShare.getShare().length,
                keyShare.getRecipient(),
                e
            );
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<NonceResponse> createNonce(
        String shareId,
        String xSessionToken,
        String xSessionCert,
        Object body
    ) {
        log.trace(
            "createNonce(shareId={},xSessionToken={},xSessionCert={} body={})",
            shareId,
            xSessionToken,
            xSessionCert,
            body
        );

        try {
            validateSessionToken.execute(xSessionToken, xSessionCert);
        } catch (VerificationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED.value()).build();
        }

        Optional<KeyShareDb> keyShare = this.keyShareRepository.findById(shareId);
        if (keyShare.isEmpty()) {
            log.error("Key share with shareId {} not found", shareId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        try {
            var saved = this.shareNonceRepository.save(
                new KeyShareNonceDb().setShareId(keyShare.get().getShareId())
            );

            log.info("KeyShareNonce(shareId = {}, nonce = {}) created", shareId, saved.getNonce());

            return ResponseEntity.ok(createNonceResponse(saved.getNonce()));
        } catch (Exception e) {
            log.error(
                "Failed to create key share nonce for share ID {}", shareId, e
            );
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<KeyShare> getKeyShareByShareId(
        String shareId,
        String xAuthToken,
        String xAuthCert,
        String xSessionToken,
        String xSessionCert,
        String xSidRpv3SignatureParameters
    ) {
        // openapi generator adds check for @NotNull, but not for isEmpty()
        // X509CertUtils.parseWithException will return null, when cert is empty string ("")
        if (xAuthCert == null || xAuthCert.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        // empty ("") xAuthToken will eventually fail with IllegalArgumentException (401) when parsing sd-jwt
        // Fail here fast and be consistent with empty xAuthCert
        if (xAuthToken == null || xAuthToken.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        try {
            validateSessionToken.execute(xSessionToken, xSessionCert);
        } catch (VerificationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED.value()).build();
        }

        // check xAuthToken
        String tokenRecipient; // "etsi/PNOEE-30303039914"
        try {
            if (certificateConfig.signCertForbidden()) {
                // parseWithException returns null, when cert is empty string ("")
                X509Certificate cert = X509CertUtils.parseWithException(
                    Base64.getUrlDecoder().decode(xAuthCert)
                );
                verifyCertificateUsagePurpose(cert);
            }
            tokenRecipient = validateAuthToken(
                shareId,
                xAuthToken,
                xSidRpv3SignatureParameters,
                xAuthCert
            );
        } catch (CertificateException | VerificationException ex) {
            if (log.isDebugEnabled()) {
                log.debug("Auth validation has failed", ex);
            } else {
                log.info("Auth validation has failed with {}", ex.getMessage());
            }

            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        Optional<KeyShareDb> shareDbOpt = this.keyShareRepository.findById(shareId);
        if (shareDbOpt.isEmpty()) {
            log.debug("Key share with shareId {} not found", shareId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        KeyShareDb shareDb = shareDbOpt.get();
        //check that keyShare can be accessed by auth token issuer
        if (!tokenRecipient.equals(shareDb.getRecipient())) {
            log.warn("Key share with shareId {} and recipient {} doesn't match token issuer {}",
                shareId, shareDb.getRecipient(), tokenRecipient);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return ResponseEntity.ok(createKeyShare(shareDb));
    }

    private static KeyShare createKeyShare(KeyShareDb share) {
        var response = new KeyShare();
        response.setRecipient(share.getRecipient());
        response.setShare(share.getShare());

        return response;
    }

    /**
     * Get URI for getting Key Share resource (Location).
     *
     * @param id Share id example: KC9b7036de0c9fce889850c4bbb1e23482
     * @return URI (path and query) example: /key-shares/KC9b7036de0c9fce889850c4bbb1e23482
     * @throws URISyntaxException in case of URI syntax error
     */
    private static URI getResourceLocation(String id) throws URISyntaxException {
        return getPathAndQueryPart(
            linkTo(methodOn(
                KeySharesApiController.class
                // xAuthtoken and xAuthCertificate are not part of url as these are header params
            ).getKeyShareByShareId(id, "", "", "", "", "")).toUri()
        );
    }

    /**
     * CSS server receives the compact SD-JWT presentation (<Issuer-signed JWT>~<Disclosure 1>~) and performs following
     * authentication and authorization checks:
     *
     * <ul>
     * <li>Verify that SD-JWT is signed by the key pair, whose public key is included in the certificate, presented
     * in the API method "GET /key-shares/{shareId}" parameter "x-cdoc2-auth-x5c".
     * <li>Verify that certificate is issued by trustworthy CA.
     * <li>Verify that certificate is valid at current point of time and is not revoked.
     * <li>Verify that SD-JWT contains claim aud, which is an array, which contains exactly one JSON string.
     * <li>Parse the aud value from SD-JWT (something like
     *     "https://css.example-org1.ee:443/key-shares/9EE90F2D-D946-4D54-9C3D-F4C68F7FFAE3?nonce=59b314d4815f257694b6")
     *      into components serverBaseURL, key-share and nonce.
     * <li>Verify that serverBaseURL is correct for this CSS server (actual requested URL is extracted from low level
     *     <code>nativeWebRequest</code>).
     * <li>Verify that this CSS server has previously generated a nonce for this key-share and nonce is not expired
     * <li>Verify that recipient_id (etsi/PNOEE-xyz) from the KeySharesCapsule matches with the subject SERIALNUMBER
     *     (PNOEE-xyz) from the X.509 certificate.
     * </ul>
     * <p>
     * If all checks are positive, then the authentication and access control decision is successful and CSS server can
     * return the capsule.
     *
     * @param shareId    requested shareId (will be compared to shareId in xAuthToken)
     * @param xAuthToken SD-JWT auth token that was generated for requested <code>shareId</code>
     * @param cert       X.509 certificate. Certificate subject/SERIALNUMBER must match
     *                   <code>xAuthToken</code> body "iss" without "etsi/" prefix.
     * @return "iss" of SD-JWT, represents authToken issuer identify. Example "etsi/PNOEE-30303039914"
     * @throws VerificationException   if xAuthToken validation fails
     * @throws ResponseStatusException status 404, when shareId or nonce is not found from DB or nonce is expired
     */
    protected String validateAuthToken(
        String shareId,
        String xAuthToken,
        String sidRpv3SignatureParameters,
        String cert
    ) throws VerificationException {
        KeyStore sidTrustedIssuers = sslBundles.getBundle("sid-trusted-issuers").getStores().getTrustStore();

        AuthTokenVerifier tokenVerifier = new AuthTokenVerifier(
            sidTrustedIssuers,
            certificateConfig.revocationChecksEnabled()
        );

        TokenVerificationResponse verificationResponse = tokenVerifier.verify(
            xAuthToken,
            cert,
            sidRpv3SignatureParameters,
            rpServerConfigProperties.rpName(),
            rpServerConfigProperties.schemeName()
        );


        ShareAccessData shareAccessData;
        try {
            shareAccessData = ShareAccessData.fromURL(verificationResponse.nonceUri().toURL());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        checkTokenAudience(shareId, shareAccessData);

        return verificationResponse.identifier().toString();
    }

    /**
     * Check that token "aud" claim matches url in request
     *
     * @param shareId         shareId from request
     * @param shareAccessData ShareAccessData derived from URL returned by auth token
     *                        verification response
     * @throws VerificationException if token "aud" claim validation has failed
     */
    protected void checkTokenAudience(String shareId, ShareAccessData shareAccessData)
        throws VerificationException {

        Objects.requireNonNull(shareId);

        try {
            URL reqURL = extractRequestURL(this.nativeWebRequest);
            URL tokenBaseURL = new URL(shareAccessData.getServerBaseUrl());

            // check protocol, host and port
            if ((tokenBaseURL.getHost() == null) || !tokenBaseURL.getHost().equals(reqURL.getHost())
                || tokenBaseURL.getProtocol() == null || !tokenBaseURL.getProtocol().equals(reqURL.getProtocol())
                || tokenBaseURL.getPort() != reqURL.getPort()
            ) {
                throw new VerificationException("protocol, host or port in token and request don't match ("
                    + shareAccessData.getServerBaseUrl() + "!="
                    + reqURL + ")");
            }

            if (!shareId.equals(shareAccessData.getShareId())) {
                throw new VerificationException("token and request shareId don't match");
            }

            checkNonceFromDB(shareAccessData.getShareId(), shareAccessData.getNonce());
        } catch (MalformedURLException ex) {
            log.error("Error validating \"aud\" data", ex);
            throw new VerificationException("Error validating \"aud\" data", ex);
        }
    }

    /**
     * Checks that tokenNonce exists in DB and is not older than <code>nonceExpirationSeconds</code>
     *
     * @param tokenShareId shareId extracted from sd-jwt
     * @param tokenNonce   nonce extracted from sd-jwt
     * @throws ResponseStatusException with status NOT_FOUND, when nonce is not found from DB or is expired
     */
    protected void checkNonceFromDB(String tokenShareId, String tokenNonce) {
        byte[] nonceBytes = Base64.getUrlDecoder().decode(tokenNonce);
        Optional<KeyShareNonceDb> dbNonceOpt = this.shareNonceRepository
            .findByShareIdAndNonce(tokenShareId, nonceBytes);

        if (dbNonceOpt.isPresent()) {
            KeyShareNonceDb dbNonce = dbNonceOpt.get();
            long nonceExpirationSeconds = nonceConfigProperties.expirationSeconds();
            Instant now = Instant.now();
            long nonceAgeSeconds = now.getEpochSecond() - dbNonce.getCreatedAt().getEpochSecond();
            if (nonceAgeSeconds > nonceExpirationSeconds) {
                log.debug("nonce {} is expired. now({})-nonce.createdAt({})={} > {}", tokenNonce,
                    now, dbNonce.getCreatedAt(), nonceAgeSeconds, nonceExpirationSeconds);
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
        } else {
            log.info("nonce {} not found for share {}", tokenNonce, tokenShareId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    protected static URL extractRequestURL(NativeWebRequest nativeWebRequest) throws MalformedURLException {
        if (nativeWebRequest == null) {
            throw new IllegalArgumentException("nativeRequest not initialized"); // http 500, should not happen normally
        }

        HttpServletRequest req = nativeWebRequest.getNativeRequest(HttpServletRequest.class);

        if (req != null) {
            //when running behind proxy, then "X-Forwarded-*" headers should be set
            String forwardedScheme = req.getHeader("X-Forwarded-Proto");
            String forwardedHost = req.getHeader("X-Forwarded-Host");
            String forwardedPort = req.getHeader("X-Forwarded-Port");

            String scheme = (forwardedScheme != null) ? forwardedScheme : req.getScheme(); //https
            String hostname = (forwardedHost != null) ? forwardedHost : req.getServerName();
            String port = (forwardedPort != null) ? forwardedPort : String.valueOf(req.getServerPort());

            String path = req.getRequestURI(); //without query params
            //String params = req.getQueryString(); // Query parameters, if any

            String url = scheme + "://" + hostname
                + ":" + port // port should be explicitly be part of url even for port 80/443
                + path;
            //+ ((params == null) ? "": "?" + params)

            log.debug("Request url {}", url);
            return new URL(url);
        } else {
            log.error("Failed to convert nativeWebRequest to HttpServletRequest");
        }

        throw new MalformedURLException("Failed to extract request URL");
    }

    /**
     * Checks user certificate purpose and verifies that signing certificate is not used for
     * authentication.
     *
     * @param cert certificate to check. {@link KeyUsage#nonRepudiation} value of keyUsage means
     *             that certificate has a signing purpose. Authentication purpose keyUsage would be
     *             {@link KeyUsage#digitalSignature} or {@link KeyUsage#keyEncipherment} or
     *             {@link KeyUsage#dataEncipherment}.
     * @throws VerificationException if certificate is not valid for authentication or its purpose
     *                               extraction has failed
     */
    private void verifyCertificateUsagePurpose(X509Certificate cert) throws VerificationException {
        boolean[] keyUsages = cert.getKeyUsage();
        if (null == keyUsages) {
            log.error("Certificate has no keyUsage");
            throw new VerificationException(
                "Required extension keyUsage for certificate purpose is missing"
            );
        }
        final int signingCertBits = KeyUsage.nonRepudiation;
        try {
            Certificate bcCert = Certificate.getInstance(ASN1Primitive.fromByteArray(cert.getEncoded()));
            X509CertificateHolder bcX509Cert = new X509CertificateHolder(bcCert);
            boolean isSigningCert =
                KeyUsage.fromExtensions(bcX509Cert.getExtensions()).hasUsages(signingCertBits);
            if (isSigningCert) {
                String errMsg = "Signing certificate cannot be used for authentication";
                log.error(errMsg);
                throw new VerificationException("Signing certificate cannot be used for authentication");
            }
        } catch (CertificateEncodingException | IOException ex) {
            String errMsg = "Failed to check certificate usage purpose";
            log.error(errMsg);
            throw new VerificationException(errMsg, ex);
        }
    }

}
