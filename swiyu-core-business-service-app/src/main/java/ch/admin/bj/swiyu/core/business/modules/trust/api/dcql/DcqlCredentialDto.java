package ch.admin.bj.swiyu.core.business.modules.trust.api.dcql;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

/**
 * Represents a Credential Query within a DCQL query.
 * A Credential Query is an object representing a request for a presentation of one or more matching Credentials.
 *
 * @see <a href="https://openid.net/specs/openid-4-verifiable-presentations-1_0.html#section-6.1">OpenID for Verifiable Presentations 1.0, Section 6.1</a>
 */
@Schema(
    description = "Represents a Credential Query within a DCQL query according to " +
        "https://openid.net/specs/openid-4-verifiable-presentations-1_0.html#section-6.1"
)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DcqlCredentialDto(
    @Schema(
        description = "An object defining additional properties requested by the Verifier that apply to " +
            "the metadata and validity data of the Credential. The properties of this object are defined " +
            "per Credential Format. " +
            "According to OpenID for Verifiable Presentations 1.0, Section 6.1, property 'meta'.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @JsonProperty("meta")
    @NotNull(message = "meta is required")
    @Valid
    DcqlCredentialMetaDto meta, // REQUIRED

    @Schema(hidden = true) // do not render into swagger, but keep values
    @JsonAnySetter
    @JsonAnyGetter
    Map<String, Object> additionalProperties
) {}
