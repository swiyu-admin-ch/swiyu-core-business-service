package ch.admin.bj.swiyu.core.business.modules.identifier.service;

import ch.admin.bj.swiyu.core.business.common.api.IdentifierStatusDto;
import ch.admin.bj.swiyu.core.business.modules.identifier.api.IdentifierEntryDto;
import ch.admin.bj.swiyu.core.business.modules.identifier.domain.IdentifierStatus;
import ch.admin.bj.swiyu.registry.identifier.IdentifierRegistryProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IdentifierEntryMapper {

    private final IdentifierRegistryProperties identifierRegistryProperties;

    public IdentifierEntryDto toIdentifierEntryDto(
        ch.admin.bj.swiyu.core.business.modules.identifier.domain.IdentifierEntry identifierEntry
    ) {
        return IdentifierEntryDto.builder()
            .id(identifierEntry.getId())
            .updatedAt(identifierEntry.getAuditMetadata().getLastModifiedAt())
            .createdAt(identifierEntry.getAuditMetadata().getCreatedAt())
            .did(identifierEntry.getDid())
            .description(identifierEntry.getDescription())
            .status(toIdentifierStatusDto(identifierEntry.getStatus()))
            .identifierRegistryUrl(
                identifierRegistryProperties.defaultPublicResolveUrlTemplate().formatted(identifierEntry.getId())
            )
            .build();
    }

    private static IdentifierStatusDto toIdentifierStatusDto(IdentifierStatus status) {
        return switch (status) {
            case NOT_INITIALIZED -> IdentifierStatusDto.NOT_INITIALIZED;
            case INITIALIZED -> IdentifierStatusDto.INITIALIZED;
            case USER_DEACTIVATED -> IdentifierStatusDto.USER_DEACTIVATED;
            case DEACTIVATED_BY_MIGRATION_BECAUSE_OF_UNSUPPORTED_FORMAT -> IdentifierStatusDto.DEACTIVATED_BY_MIGRATION_BECAUSE_OF_UNSUPPORTED_FORMAT;
        };
    }
}
