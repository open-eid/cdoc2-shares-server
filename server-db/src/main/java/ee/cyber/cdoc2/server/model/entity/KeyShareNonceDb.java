package ee.cyber.cdoc2.server.model.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.security.NoSuchAlgorithmException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import ee.cyber.cdoc2.shared.crypto.Crypto;


/**
 * Key material share nonce database entity
 */
@Data
@Entity
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Table(name = "key_material_share_nonce")
public class KeyShareNonceDb extends AuditEntity {

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

    @NotNull
    @Column
    @Size(min = 18, max = 34)
    private String shareId;

    @NotNull
    @Column
    @Size(min = 12, max = 16)
    @JdbcTypeCode(SqlTypes.BINARY)
    private byte[] nonce;

}
