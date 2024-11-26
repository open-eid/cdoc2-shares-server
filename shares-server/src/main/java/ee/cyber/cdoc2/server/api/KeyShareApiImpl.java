package ee.cyber.cdoc2.server.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

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

import static ee.cyber.cdoc2.server.Utils.getPathAndQueryPart;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;


/**
 * Implements API for getting and creating CDOC2 key shares {@link KeySharesApi}
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class KeyShareApiImpl implements KeySharesApiDelegate {

    private final NativeWebRequest nativeWebRequest;

    private final KeyShareRepository keyShareRepository;

    private final KeyShareNonceRepository shareNonceRepository;

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
                new KeyShareNonceDb()
                    .setShareId(keyShare.get().getShareId())
            );

            log.info("KeyShareNonce(shareId = {}, nonce = {}) created", shareId, saved.getNonce());

            return ResponseEntity.ok(createNonceResponse(saved.getNonce()));
        } catch (Exception e) {
            log.error(
                "Failed to create key share nonce for share ID {}", shareId,
                e
            );
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<KeyShare> getKeyShareByShareId(String shareId, byte[] xAuthTicket) {
        Optional<KeyShareDb> shareDbOpt = this.keyShareRepository.findById(shareId);
        if (shareDbOpt.isEmpty()) {
            log.debug("Key share with shareId {} not found", shareId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return ResponseEntity.ok(createKeyShare(shareDbOpt.get()));
    }

    private static KeyShare createKeyShare(KeyShareDb share) {
        var response = new KeyShare();
        response.setRecipient(share.getRecipient());
        response.setShare(share.getShare());

        return response;
    }

    private static NonceResponse createNonceResponse(byte[] nonce) {
        var response = new NonceResponse();
        response.setNonce(nonce);

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
                // ToDo replace xAuthTicket bytes with auth token after implementation
            ).getKeyShareByShareId(id, new byte[32])).toUri()
        );
    }

}
