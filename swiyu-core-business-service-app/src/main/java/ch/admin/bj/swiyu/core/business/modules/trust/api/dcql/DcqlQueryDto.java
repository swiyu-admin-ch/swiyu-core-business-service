package ch.admin.bj.swiyu.core.business.modules.trust.api.dcql;

import com.fasterxml.jackson.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;

/**
 * Represents the Digital Credentials Query Language (DCQL) query.
 * DCQL is a JSON-encoded query language that allows the Verifier to request Presentations that match the query.
 * The Verifier MAY encode constraints on the combinations of Credentials and claims that are requested.
 * The Wallet evaluates the query against the Credentials it holds and returns Presentations matching the query.
 *
 * @see <a href="https://openid.net/specs/openid-4-verifiable-presentations-1_0.html#section-6">OpenID for Verifiable Presentations 1.0, Section 6</a>
 */
@Schema(
    description = """
    Must represent a Digital Credentials Query Language (DCQL) query according to https://openid.net/specs/openid-4-verifiable-presentations-1_0.html#section-6
    It is only validated that for every credential query, a "meta" field with an object containing at least the field "vct_values" with a non-empty array exists.""",
    example = """
    {
      "credentials": [
        {
          "id": "identity_credential_dcql",
          "format": "dc+sd-jwt",
          "meta": {
            "vct_values": ["https://credentials.example.com/identity_credential"]
          },
          "claims": [
            { "path": ["given_name"] },
            { "path": ["family_name"] },
            { "path": ["address", "street_address"] }
          ],
          "require_cryptographic_holder_binding": true
        },
        {
          "id": "university_degree_dcql",
          "format": "dc+sd-jwt",
          "meta": {
            "vct_values": ["https://credentials.example.com/university_degree"]
          },
          "claims": [
            { "path": ["degree_name"] },
            { "path": ["graduation_year"] }
          ]
        }
      ],
      "credential_sets": [
        {
          "options": [
            ["identity_credential_dcql"],
            ["university_degree_dcql"]
          ],
          "required": true
        }
      ]
    }
    """
)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DcqlQueryDto(
    @Schema(
        description = "A non-empty array of Credential Queries that specify the requested Credentials. " +
            "According to OpenID for Verifiable Presentations 1.0, Section 6, property 'credentials'.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @JsonProperty("credentials")
    @Valid
    @NotEmpty(message = "credentials must not be empty")
    List<DcqlCredentialDto> credentials,

    @Schema(hidden = true) // do not render into swagger, but keep values
    @JsonAnySetter
    @JsonAnyGetter
    Map<String, Object> additionalProperties
) {}
