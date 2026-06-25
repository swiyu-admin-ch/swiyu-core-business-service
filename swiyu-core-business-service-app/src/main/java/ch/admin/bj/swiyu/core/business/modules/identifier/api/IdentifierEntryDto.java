package ch.admin.bj.swiyu.core.business.modules.identifier.api;

import ch.admin.bj.swiyu.core.business.common.api.IdentifierStatusDto;
import ch.admin.bj.swiyu.core.business.common.api.ListItemDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Schema(name = "IdentifierEntry")
@Builder
public record IdentifierEntryDto(
    @Schema(example = "18fa7c77-9dd1-4e20-a147-fb1bec146085") UUID id,
    @Schema(example = "2024-10-29T09:35:16.809924Z") Instant createdAt,
    @Schema(example = "2024-10-29T09:35:16.809924Z") Instant updatedAt,
    String did,
    String description,
    IdentifierStatusDto status,
    @Schema(
        example = "https://identifier-reg.trust-infra.swiyu.admin.ch/api/v1/did/18fa7c77-9dd1-4e20-a147-fb1bec146085"
    )
    String identifierRegistryUrl
) implements ListItemDto {}
