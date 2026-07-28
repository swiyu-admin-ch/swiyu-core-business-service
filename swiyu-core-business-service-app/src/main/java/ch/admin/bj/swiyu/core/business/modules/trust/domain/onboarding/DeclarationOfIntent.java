package ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Embeddable
@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PUBLIC) // JPA
public class DeclarationOfIntent {

    @NotNull
    private UUID fullySignedDocumentId;

    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode validationReport;
}
