package ch.admin.bj.swiyu.core.business.modules.identifier.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import lombok.Builder;

/**
 * Filter parameters for querying identifier entries.
 *
 * @param businessPartnerId return only entries with for this partner
 * @param activeOnly        return only entries with uploaded did
 */
@Builder
@Schema(name = "IdentifierEntry")
public record IdentifierEntryFilterDto(UUID businessPartnerId, Boolean activeOnly) {}
