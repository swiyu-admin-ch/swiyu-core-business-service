package ch.admin.bj.swiyu.core.business.modules.trust.api.dcql;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;

/**
 * Represents metadata parameters within a Credential Query.
 * The properties of this object are defined per Credential Format.
 *
 * @see <a href="https://openid.net/specs/openid-4-verifiable-presentations-1_0.html#section-6.1">OpenID for Verifiable Presentations 1.0, Section 6.1, property 'meta'</a>
 * @see <a href="https://openid.net/specs/openid-4-verifiable-presentations-1_0.html#name-parameters-in-the-meta-para">OpenID for Verifiable Presentations 1.0, Appendix B.1.1 (W3C VCs)</a>
 * @see <a href="https://openid.net/specs/openid-4-verifiable-presentations-1_0.html#sd_jwt_vc_meta_parameter">OpenID for Verifiable Presentations 1.0, Appendix B.2.3 (ISO mdoc)</a>
 * @see <a href="https://openid.net/specs/openid-4-verifiable-presentations-1_0.html#mdocs_meta_parameter">OpenID for Verifiable Presentations 1.0, Appendix B.3.5 (IETF SD-JWT VC)</a>
 */
@Schema(
    description = "Represents metadata parameters within a Credential Query according to " +
        "https://openid.net/specs/openid-4-verifiable-presentations-1_0.html#section-6.1"
)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DcqlCredentialMetaDto(
    @Schema(
        description = "For IETF SD-JWT VC: A non-empty array of strings that specifies allowed " +
            "values for the type of the requested Verifiable Credential. " +
            "According to OpenID for Verifiable Presentations 1.0, Appendix B.3.5, property 'vct_values'."
    )
    @JsonProperty("vct_values")
    @Valid
    @NotEmpty(message = "every credential query must contain a non-empty vct_values array")
    List<@NotEmpty String> vctValues,

    @Schema(hidden = true) // do not render into swagger, but keep values
    @JsonAnySetter
    @JsonAnyGetter
    Map<String, Object> additionalProperties
) {}
