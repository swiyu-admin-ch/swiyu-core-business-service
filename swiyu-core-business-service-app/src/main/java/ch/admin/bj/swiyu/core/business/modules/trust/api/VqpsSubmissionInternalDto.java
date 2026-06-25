package ch.admin.bj.swiyu.core.business.modules.trust.api;

import ch.admin.bj.swiyu.core.business.common.i18n.ValidLocalizedMap;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Schema(name = "VqpsSubmissionInternal")
public record VqpsSubmissionInternalDto(
    @NotNull UUID id,
    @NotNull UUID partnerId,
    @NotNull Long version,
    @NotNull VqpsSubmissionStatusDto status,
    @NotBlank String sub,
    @JsonProperty("purpose_name") @NotNull @NotEmpty @ValidLocalizedMap Map<String, String> purposeName,

    @Schema(
        description = "Description of the purpose",
        requiredMode = Schema.RequiredMode.REQUIRED // @NotBlank is ignored by springdoc openapi
    )
    @JsonProperty("purpose_description")
    @NotNull
    @NotEmpty
    @ValidLocalizedMap
    Map<String, String> purposeDescription,
    @NotBlank String scope,
    @NotNull JsonNode query,
    VqpsPublicationFailureReasonDto failureReason,
    Instant createdAt,
    Instant updatedAt
) {}
