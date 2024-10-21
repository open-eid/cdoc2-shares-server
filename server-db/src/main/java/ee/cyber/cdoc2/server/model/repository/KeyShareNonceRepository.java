package ee.cyber.cdoc2.server.model.repository;


import org.springframework.data.jpa.repository.JpaRepository;

import ee.cyber.cdoc2.server.model.entity.KeyShareNonceDb;


public interface KeyShareNonceRepository extends JpaRepository<KeyShareNonceDb, Long> {
}

