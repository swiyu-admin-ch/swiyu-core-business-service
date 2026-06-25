package ch.admin.bj.swiyu.core.business.modules.trust.domain.vqps;

import jakarta.persistence.Embeddable;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Embeddable
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PUBLIC) // JPA
public class VqpsPublicationResult {

    private UUID jti;
    private String jwt;
    private Instant expiresAt;
}
