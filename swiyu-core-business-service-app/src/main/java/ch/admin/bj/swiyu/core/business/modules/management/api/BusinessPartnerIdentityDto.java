package ch.admin.bj.swiyu.core.business.modules.management.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Schema(name = "BusinessPartnerIdentity")
public record BusinessPartnerIdentityDto(
    @Schema(description = "Date until which the identity is valid") Instant validUntil,
    @Schema(description = "List of trusted identifiers (DIDs) associated with this identity")
    List<String> trustedIdentifier,
    @Schema(description = "Current activation status of the business partner identity")
    BusinessPartnerIdentityStatusDto status,
    @Schema(description = "Timestamp when the identity was last activated") Instant lastActivated,
    @Schema(description = "Enterprise identification number (UID)") String uid,
    @Schema(description = "Localized entity name as managed by the trust management service")
    Map<String, String> entityName
) {}
