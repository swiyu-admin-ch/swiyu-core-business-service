package ch.admin.bj.swiyu.core.business.modules.status.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import lombok.Builder;

@Builder
public record StatusListEntryCreationDto(
    @Schema(example = "18fa7c77-9dd1-4e20-a147-fb1bec146085") UUID id,
    @Schema(example = "https://status-registry.admin.ch/api/v1/statuslist/18fa7c77-9dd1-4e20-a147-fb1bec146085.jwt")
    String statusRegistryUrl
) {}
