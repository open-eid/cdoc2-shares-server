package ee.cyber.cdoc2.server.model.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import ee.cyber.cdoc2.server.model.Crypto;


/**
 * Key material share database entity
 */
@Data
@Entity
@Table(name = "key_material_share")
@EntityListeners(AuditingEntityListener.class)
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class KeyShareDb extends AuditEntity {

    @PrePersist
    private void generateShareId() throws NoSuchAlgorithmException {
        byte[] sRnd = new byte[16];
        Crypto.getSecureRandom().nextBytes(sRnd);
        this.shareId = HexFormat.of().formatHex(sRnd);
    }

    @Id
    @Column(unique = true)
    @Size(min = 18, max = 34)
    private String shareId;

    @NotNull
    @Column
    @Size(min = 12, max = 32)
    private String recipient;

    @NotNull
    @Column
    @Size(min = 32, max = 128)
    @JdbcTypeCode(SqlTypes.BINARY)
    private byte[] share;

}
