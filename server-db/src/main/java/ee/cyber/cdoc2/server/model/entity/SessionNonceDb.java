package ee.cyber.cdoc2.server.model.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;


/**
 * Session nonce database entity
 */
@Data
@Entity
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Table(name = "session_nonce")
public class SessionNonceDb extends AuditEntity {

    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.BINARY)
    private byte[] nonce;

}

