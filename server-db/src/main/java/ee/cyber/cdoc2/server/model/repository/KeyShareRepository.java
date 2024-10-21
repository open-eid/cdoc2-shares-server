package ee.cyber.cdoc2.server.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import ee.cyber.cdoc2.server.model.entity.KeyShareDb;


public interface KeyShareRepository extends JpaRepository<KeyShareDb, String> {
}

