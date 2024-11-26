package ee.cyber.cdoc2.server.model.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;


@MappedSuperclass
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class AuditEntity {

    @CreatedDate
    private Instant createdAt;

}
