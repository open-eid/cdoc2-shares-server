package ee.cyber.cdoc2.server.model.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.security.NoSuchAlgorithmException;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import ee.cyber.cdoc2.server.model.Crypto;

/**
 * Session nonce database entity
 */
@Data
@Entity
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Table(name = "session_nonce")
public class SessionNonceDb extends AuditEntity {

    @PrePersist
    private void generateNonce() throws NoSuchAlgorithmException {
        byte[] sRnd = new byte[16];
        Crypto.getSecureRandom().nextBytes(sRnd);
        this.nonce = sRnd;
    }

    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 16)
    @JdbcTypeCode(SqlTypes.BINARY)
    private byte[] nonce;

}

