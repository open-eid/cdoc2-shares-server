package ee.cyber.cdoc2.server.api;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.util.X509CertUtils;
import ee.cyber.cdoc2.auth.AuthTokenVerifier;
import ee.cyber.cdoc2.auth.IllegalCertificateException;
import ee.cyber.cdoc2.auth.SIDCertificateUtil;
import ee.cyber.cdoc2.auth.ShareAccessData;
import ee.cyber.cdoc2.auth.VerificationException;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.NativeWebRequest;

import ee.cyber.cdoc2.server.generated.api.KeySharesApi;
import ee.cyber.cdoc2.server.generated.api.KeySharesApiController;
import ee.cyber.cdoc2.server.generated.api.KeySharesApiDelegate;
import ee.cyber.cdoc2.server.generated.model.KeyShare;
import ee.cyber.cdoc2.server.generated.model.NonceResponse;
import ee.cyber.cdoc2.server.model.entity.KeyShareDb;
import ee.cyber.cdoc2.server.model.entity.KeyShareNonceDb;
import ee.cyber.cdoc2.server.model.repository.KeyShareNonceRepository;
import ee.cyber.cdoc2.server.model.repository.KeyShareRepository;
import org.springframework.web.server.ResponseStatusException;

import static ee.cyber.cdoc2.auth.Constants.NONCE;
import static ee.cyber.cdoc2.auth.Constants.SERVER_BASE_URL;
import static ee.cyber.cdoc2.auth.Constants.SHARE_ACCESS_DATA;
import static ee.cyber.cdoc2.auth.Constants.SHARE_ID;
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

    private static final String VALUE_IS_MISSING = "value is missing";

    private final NativeWebRequest nativeWebRequest;

    private final KeyShareRepository keyShareRepository;

    private final KeyShareNonceRepository shareNonceRepository;

    // configure sslBundles in application.properties
    // https://docs.spring.io/spring-boot/reference/features/ssl.html#features.ssl.pem
    private final SslBundles sslBundles;

    @Value("${cdoc2.auth-x5c.revocation-checks.enabled:false}")
    private boolean revocationCheckEnabled;

    @Value("${cdoc2.nonce.expiration.seconds:300}")
    private long nonceExpirationSeconds;

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
    public ResponseEntity<NonceResponse> createNonce(String shareId, Object body) {
        log.trace("createNonce(shareId={}, body={})", shareId, body);
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
        String xAuthTicket,
        String xAuthCert
) {
        // check xAuthTicket
        String ticketRecipient; // "etsi/PNOEE-30303039914"
        try {
            ticketRecipient = validateAuthTicket(shareId, xAuthTicket, xAuthCert);
        } catch (VerificationException ve) {

            if (log.isDebugEnabled()) {
                log.debug("Auth ticket validation failed", ve);
            } else {
                log.info("Auth ticket validation failed with {}", ve.getMessage());
            }

            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        Optional<KeyShareDb> shareDbOpt = this.keyShareRepository.findById(shareId);
        if (shareDbOpt.isEmpty()) {
            log.debug("Key share with shareId {} not found", shareId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        KeyShareDb shareDb =  shareDbOpt.get();
        //check that keyShare can be accessed by auth ticket issuer
        if (!ticketRecipient.equals(shareDb.getRecipient())) {
            log.warn("Key share with shareId {} and recipient {} doesn't match ticket issuer {}",
                shareId, shareDb.getRecipient(), ticketRecipient);
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

    private static NonceResponse createNonceResponse(byte[] nonce) {
        var response = new NonceResponse();
        response.setNonce(base64UrlEnc(nonce));

        return response;
    }

    /**
     * Get URI for getting Key Share resource (Location).
     * @param id Share id example: KC9b7036de0c9fce889850c4bbb1e23482
     * @return URI (path and query) example: /key-shares/KC9b7036de0c9fce889850c4bbb1e23482
     * @throws URISyntaxException in case of URI syntax error
     */
    private static URI getResourceLocation(String id) throws URISyntaxException {
        return getPathAndQueryPart(
            linkTo(methodOn(
                KeySharesApiController.class
                // xAuthTicket and xAuthCertificate are not part of url as these are header params
            ).getKeyShareByShareId(id, "", "")).toUri()
        );
    }

    private static String base64UrlEnc(byte[] src) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(src);
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
     *
     * If all checks are positive, then the authentication and access control decision is successful and CSS server can
     * return the capsule.
     * @param shareId requested shareId (will be compared to shareId in authTicket)
     * @param xAuthTicket SD-JWT authticket that was generated for requested <code>shareId</code>
     * @param x5c <code>xAuthTicket</code> signer certificate in PEM format. Certificate subject/SERIALNUMBER must match
     *            <code>xAuthTicket</code> header "kid"
     * @return "iss" of SD-JWT, represents authToken issuer identify. Example "etsi/PNOEE-30303039914"
     * @throws VerificationException if authTicket validation fails
     * @throws ResponseStatusException status 404, when shareId or nonce is not found from DB or nonce is expired
     */
    protected String validateAuthTicket(String shareId, String xAuthTicket, String x5c)
        throws VerificationException {

        Map<String, Object> verifiedClaims;

        // check that SD-JWT is signed with x5c
        try {
            X509Certificate cert = X509CertUtils.parseWithException(x5c);
            // check that x5c is issued by trustworthy CA
            checkCertificateIssuer(cert);

            // check that certificate subject.serialnumber matches to sdjwt.header.kid
            // signature is valid
            // disclose hidden claims
            verifiedClaims = AuthTokenVerifier.getVerifiedClaims(xAuthTicket, cert,
                SIDCertificateUtil::getSemanticsIdentifier);
            log.debug("claims: {}", verifiedClaims);

            // check that "iss" matches subject/serialnumber in cert
            String ticketIss = obj2String(verifiedClaims.get("iss"));
            String certSubjectSerial = SIDCertificateUtil.getSemanticsIdentifier(cert);
            checkIssuerMatchesCertSubject(ticketIss, certSubjectSerial);
        } catch (JOSEException | ParseException | CertificateException | IllegalCertificateException e) {
            throw new VerificationException("Ticket processing error", e);
        }

        // check aud url - this will change as currently it has shareAccessData structure
        checkTicketAudience(shareId, verifiedClaims);

        return obj2String(verifiedClaims.get("iss"));
    }

    /**
     * For SID cert, check that certificate subject.serialnumber matches with authTicket "iss" claim
     * @param ticketIssuer "iss" value from authTicket, example "etsi/PNOEE-30303039914"
     * @param certSubjectSerial serialnumber from certificate, example "PNOEE-30303039914"
     * @throws VerificationException if subject.serialnumber validation has failed
     */
    protected void checkIssuerMatchesCertSubject(@Nullable String ticketIssuer, String certSubjectSerial)
        throws VerificationException {

        if ((ticketIssuer == null) || !ticketIssuer.startsWith("etsi/")) {
            throw new VerificationException("\"iss\" does not start \"etsi/\"");
        }

        if (!ticketIssuer.substring("etsi/".length()).equals(certSubjectSerial)) {
            throw new VerificationException("subject serial "
                + certSubjectSerial + " doesn't match to iss " + ticketIssuer);
        }
    }

    protected void checkCertificateIssuer(X509Certificate cert) throws VerificationException {

        // enable certpath validation debug logging by setting security property
        // -Djava.security.debug=certpath.ocsp,ocsp,verbose

        try {
            KeyStore trustStore = sslBundles.getBundle("sid-trusted-issuers").getStores().getTrustStore();

            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            CertPath certPath = cf.generateCertPath(Collections.singletonList(cert));

            Security.setProperty("com.sun.security.enableCRLDP", "false");
            Security.setProperty("ocsp.enable", "true");
            // Initialize PKIXParameters
            PKIXParameters pkixParams = new PKIXParameters(trustStore);

            // SK ocsp demo env is a minefield 💣
            // https://github.com/SK-EID/ocsp/wiki/SK-OCSP-Demo-environment
            // experimental, doesn't work for
            // TODO: RM-3218: implement properly including client side OCSP stapling support
            // enable/disable OCSP checking through application.properties
            pkixParams.setRevocationEnabled(revocationCheckEnabled);

            CertPathValidator validator = CertPathValidator.getInstance("PKIX");

            validator.validate(certPath, pkixParams); // if the CertPath does not validate,
                                                      // an CertPathValidatorException will be thrown
            Date now = new Date();
            if (now.after(cert.getNotAfter())) {
                throw new VerificationException("Certificate expired on " + cert.getNotAfter());
            }

        } catch (NoSuchAlgorithmException | CertificateException | CertPathValidatorException | KeyStoreException
                 | InvalidAlgorithmParameterException e) {
            throw new VerificationException("Certificate validation error", e);
        }
    }

    /**
     * Check that ticket "aud" claim matches url in request
     * @param shareId shareId from request
     * @param verifiedClaims disclosed claims from auth ticket
     * @throws VerificationException if ticket "aud" claim validation has failed
     */
    protected void checkTicketAudience(String shareId, Map<String, Object> verifiedClaims)
        throws VerificationException {

        Objects.requireNonNull(shareId);

        try {
            ShareAccessData shareAccessData = extractShareAccessData(verifiedClaims);
            URL reqURL = extractRequestURL(this.nativeWebRequest);
            URL ticketBaseURL = new URL(shareAccessData.getServerBaseUrl());

            // check protocol, host and port
            if ((ticketBaseURL.getHost() == null) || !ticketBaseURL.getHost().equals(reqURL.getHost())
                || ticketBaseURL.getProtocol() == null || !ticketBaseURL.getProtocol().equals(reqURL.getProtocol())
                || ticketBaseURL.getPort() != reqURL.getPort() //TODO: handle default ports
            ) {
                throw new VerificationException("protocol, host or port in ticket and request don't match ("
                    + shareAccessData.getServerBaseUrl() + "!="
                    + reqURL + ")");
            }

            if (!shareId.equals(shareAccessData.getShareId())) {
                throw new VerificationException("ticket and request shareId don't match");
            }

            checkNonceFromDB(shareAccessData.getShareId(), shareAccessData.getNonce());
        } catch (MalformedURLException | ParseException ex) {
            log.error("Error validating \"aud\" data", ex);
            throw new VerificationException("Error validating \"aud\" data", ex);
        }
    }

    /**
     * Checks that ticketNonce exists in DB and is not older than <code>nonceExpirationSeconds</code>
     * @param ticketShareId shareId extracted from sd-jwt
     * @param ticketNonce nonce extracted from sd-jwt
     * @throws ResponseStatusException with status NOT_FOUND, when nonce is not found from DB or is expired
     */
    protected void checkNonceFromDB(String ticketShareId, String ticketNonce) {
        byte[] nonceBytes = Base64.getUrlDecoder().decode(ticketNonce);
        Optional<KeyShareNonceDb> dbNonceOpt = this.shareNonceRepository
            .findByShareIdAndNonce(ticketShareId, nonceBytes);

        if (dbNonceOpt.isPresent()) {
            KeyShareNonceDb dbNonce = dbNonceOpt.get();
            long nonceAgeSeconds = Instant.now().getEpochSecond() - dbNonce.getCreatedAt().getEpochSecond();
            if (nonceAgeSeconds > this.nonceExpirationSeconds) {
                log.debug("nonce {} is expired. now({})-nonce.createdAt({})={} > {}", ticketNonce, Instant.now(),
                    dbNonce.getCreatedAt(), nonceAgeSeconds,
                    this.nonceExpirationSeconds);
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
        } else {
            log.info("nonce {} not found for share {}", ticketNonce, ticketShareId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * <code>jsonMap</code> is following json:
     * <pre>
     *         {
     *          "shareAccessData":[{
     *            "shareId":"ff0102030405060708090a0b0c0e0dff",
     *            "serverNonce":"AAECAwQFBgcICQoLDA4N_w",
     *            "serverBaseURL":"https://localhost:8443"
     *          }]
     *         }
     * </pre>
     * convert to Map using Jackson <code>ObjectMapper</code>:
     * <code>
     *     Map<String, Object> map = objectMapper.readValue(jsonString, new TypeReference<Map<String, Object>>() {});
     * </code>
     * @param jsonMap JSON map
     * @return ShareAccessData object
     * @throws ParseException if parsing has failed
     */
    ShareAccessData extractShareAccessData(Map<String, Object> jsonMap) throws ParseException {

        Object listObj = jsonMap.get(SHARE_ACCESS_DATA);
        log.debug("sd-jwt {}={}", SHARE_ACCESS_DATA, listObj);
        if (listObj instanceof Collection<?> collection) {
            if (collection.size() != 1) {
                throw new ParseException("Expected \"" + SHARE_ACCESS_DATA + "\" to be an array", 0);
            }

            Object firstObj = collection.toArray()[0];
            if (firstObj instanceof Map<?, ?> map) {
                String baseURL = obj2String(map.get(SERVER_BASE_URL));
                if (baseURL == null) {
                    throw new ParseException("\"" + SERVER_BASE_URL + "\" " + VALUE_IS_MISSING, 0);
                }
                String shareId = obj2String(map.get(SHARE_ID));
                if (shareId == null) {
                    throw new ParseException("\"" + SHARE_ID + "\" " + VALUE_IS_MISSING, 0);
                }
                String nonce = obj2String(map.get(NONCE));
                if (nonce == null) {
                    throw new ParseException("\"" + NONCE + "\" " + VALUE_IS_MISSING, 0);
                }

                return new ShareAccessData(baseURL, shareId, nonce);
            }
        }

        throw new ParseException("Failed to parse \"" + SHARE_ACCESS_DATA + "\"", 0);
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
                + (("80".equals(port) || "443".equals(port)) ? "" : ":" + port)
                + path;
                //+ ((params == null) ? "": "?" + params)

            log.debug("Request url {}", url);
            return new URL(url);
        } else {
            log.error("Failed to convert nativeWebRequest to HttpServletRequest");
        }

        throw new MalformedURLException("Failed to extract request URL");
    }

    private static String obj2String(Object obj) {
        return (obj == null) ? null : obj.toString();
    }

}
