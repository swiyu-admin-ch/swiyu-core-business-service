package ch.admin.bj.swiyu.core.business.modules.management.api;

import ch.admin.bj.swiyu.core.business.common.api.AddressDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

/**
 * Minimal business partner data needed for payment receipt generation.
 * Defined in management.api to avoid circular dependencies between modules.
 */
@Schema(name = "BusinessPartnerReceiptData", description = "Minimal business partner data for receipt generation")
public record BusinessPartnerReceiptData(
    @Schema(description = "Localized entity name map with required default key and BCP-47 locale keys")
    Map<String, String> entityName,

    @Schema(description = "Enterprise identification number of the partner", example = "CHE-123.456.789") String uid,

    @Schema(description = "Address of the partner") AddressDto address
) {}
