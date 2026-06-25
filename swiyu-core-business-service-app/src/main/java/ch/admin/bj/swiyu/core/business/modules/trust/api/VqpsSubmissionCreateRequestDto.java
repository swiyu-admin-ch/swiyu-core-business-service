package ch.admin.bj.swiyu.core.business.modules.trust.api;

import ch.admin.bj.swiyu.core.business.common.i18n.ValidLocalizedMap;
import ch.admin.bj.swiyu.core.business.modules.trust.api.dcql.DcqlQueryDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

@Schema(name = "VqpsSubmissionCreateRequest")
public record VqpsSubmissionCreateRequestDto(
    @Schema(
        description = "Whether to wait for the publication result in the response. If false, the response will be returned immediately after the submission is created, without waiting for the publication result. If true, the response will be returned only after the publication to the trust registry has succeeded, failed  or a timeout occurs.",
        example = "true",
        defaultValue = "true"
    )
    @NotNull
    Boolean waitForPublication,

    @NotBlank
    @Schema(
        description = "identifier of the verifier in a format defined in the swiss-profile-anchor 1.0",
        example = "did:tdw:DEADBEEF0000000000000000000000000000000000000000000000000000000000000000000000000000000000000:identifier-data-service-d.bit.admin.ch:api:v1:did:00000000-0000-0000-0000-000000000000",
        requiredMode = Schema.RequiredMode.REQUIRED // @NotBlank is ignored by springdoc openapi
    )
    String sub,

    @Schema(
        description = "Name of the purpose (max 40 chars)",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "{\"default\": \"Purpose name\", \"de\": \"Zweckname\", \"fr\": \"Nom de l'objectif\"}"
    )
    @JsonProperty("purpose_name")
    @NotNull
    @ValidLocalizedMap
    Map<String, @NotBlank @Size(max = 40) String> purposeName,

    @Schema(
        description = "Description of the purpose (max 1000 chars)",
        requiredMode = Schema.RequiredMode.REQUIRED,
        example = "{\"default\": \"This purpose allows verification of identity\", \"de\": \"Dieser Zweck ermöglicht die Identitätsprüfung\", \"fr\": \"Cet objectif permet la vérification d'identité\"}"
    )
    @JsonProperty("purpose_description")
    @NotNull
    @NotEmpty
    @ValidLocalizedMap
    Map<String, @NotBlank @Size(max = 1000) String> purposeDescription,

    @NotBlank
    @Schema(
        description = "a string in accordance to the \"scope\" parameter defined in OpenID4VP.",
        requiredMode = Schema.RequiredMode.REQUIRED // @NotBlank is ignored by springdoc openapi)
    )
    String scope,

    @Schema(
        description = "The DCQL query",
        example = """
        {
            "credentials": [
              {
                "id": "some_identity_credential",
                "format": "dc+sd-jwt",
                "meta": {
                  "vct_values": [ "https://credentials.example.com/identity_credential" ]
                },
                "claims": [
                    {"path": ["last_name"]},
                    {"path": ["first_name"]}
                ]
              }
            ]
          }""",
        implementation = DcqlQueryDto.class
    )
    @NotNull
    JsonNode query
) {}
