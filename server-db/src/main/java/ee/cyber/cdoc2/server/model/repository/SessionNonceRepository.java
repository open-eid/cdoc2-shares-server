package ee.cyber.cdoc2.server.model.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ee.cyber.cdoc2.server.model.entity.SessionNonceDb;

public interface SessionNonceRepository extends JpaRepository<SessionNonceDb, Long> {
    Optional<SessionNonceDb> findByNonce(byte[] nonce);
}
