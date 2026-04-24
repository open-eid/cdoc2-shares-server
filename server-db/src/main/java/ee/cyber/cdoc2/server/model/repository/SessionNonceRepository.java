package ee.cyber.cdoc2.server.model.repository;


import org.springframework.data.jpa.repository.JpaRepository;

import ee.cyber.cdoc2.server.model.entity.SessionNonceDb;


public interface SessionNonceRepository extends JpaRepository<SessionNonceDb, Long> {
    boolean existsByNonce(byte[] nonce);
}
