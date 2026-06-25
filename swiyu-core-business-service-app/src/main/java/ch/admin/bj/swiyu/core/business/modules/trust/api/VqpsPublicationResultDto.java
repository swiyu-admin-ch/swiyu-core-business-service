package ch.admin.bj.swiyu.core.business.modules.trust.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

@Schema(
    name = "VqpsPublicationResult",
    description = "Result of a VQPS publication process, containing the published JWT and its metadata."
)
public record VqpsPublicationResultDto(
    @NotBlank
    @Schema(
        description = "JSON Web Token Identifier, a UUIDv4 provided by the statement issuer to facilitate easier matching in cross reference documents like the verifiers metadata. This is the same as in the jti claim of the vqpsJwt.",
        example = "f8a8ced4-d11e-4a05-ac76-57f95eba36cd"
    )
    String jti,

    @NotBlank
    @Schema(
        description = "The Verification Query Public Statement (base64Url encoded signed JWT) as it was published in the trust registry",
        example = "eyJhbGciOiJFUzI1NiIsInR5cCI6InN3aXl1LXZlcmlmaWNhdGlvbi1xdWVyeS1wdWJsaWMtc3RhdGVtZW50K2p3dCIsImtpZCI6ImRpZDpleGFtcGxlOnZlcmlmaWNhdGlvbi1zdGF0bWVudC1pc3N1ZXIja2V5LTEiLCJwcm9maWxlX3ZlcnNpb24iOiJzd2lzcy1wcm9maWxlLXRydXN0OjEuMC4wIn0" +
            ".eyJqdGkiOiIwN2YyODlkNS04YjFmLTQ2MDQtYmY3Mi01M2JkY2I3MWVlMDUiLCJzdWIiOiJkaWQ6ZXhhbXBsZTp2ZXJpZmllciIsImlhdCI6MTY5MDM2MDk2OCwiZXhwIjoxNzUzNDMyOTY4LCJwdXJwb3NlX25hbWUiOiJiZWlzcGllbCBhYmZyYWdlIiwicHVycG9zZV9uYW1lI2RlLWNoIjoiYmVpc3BpZWwgYWJmcmFnZSIsInB1cnBvc2VfZGVzY3JpcHRpb24iOiJmcmFnZSBhYiB6dW0gYmVpc3BpZWwiLCJwdXJwb3NlX2Rlc2NyaXB0aW9uI2RlLWNoIjoiZnJhZ2UgYWIgenVtIGJlaXNwaWVsIiwicmVxdWVzdCI6eyJ0eXBlIjoiRENRTCIsInNjb3BlIjoiY29tLmV4YW1wbGUuaWRlbnRpdHlDYXJkQ3JlZGVudGlhbF9wcmVzZW50YXRpb24iLCJxdWVyeSI6eyJjcmVkZW50aWFscyI6W3siaWQiOiJteV9jcmVkZW50aWFsIiwiZm9ybWF0IjoiZGMrc2Qtand0IiwibWV0YSI6eyJ2Y3RfdmFsdWVzIjpbImh0dHBzOi8vY3JlZGVudGlhbHMuZXhhbXBsZS5jb20vaWRlbnRpdHlfY3JlZGVudGlhbCJdfSwiY2xhaW1zIjpbeyJwYXRoIjpbImxhc3RfbmFtZSJdfV19XX19fQ" +
            ".<signature>"
    )
    String jwt,

    @Schema(description = "Expiry of validity time. This is the same as in the jti claim of the vqpsJwt.")
    @NotNull
    Instant expiresAt
) {}
