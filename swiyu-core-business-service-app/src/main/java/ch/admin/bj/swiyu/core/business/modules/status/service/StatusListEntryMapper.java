package ch.admin.bj.swiyu.core.business.modules.status.service;

import ch.admin.bj.swiyu.core.business.modules.status.api.StatusListEntryDto;
import ch.admin.bj.swiyu.core.business.modules.status.domain.StatusListEntry;
import lombok.experimental.UtilityClass;

@UtilityClass
public class StatusListEntryMapper {

    public static StatusListEntryDto toStatusListEntryDto(StatusListEntry statusListEntry) {
        return StatusListEntryDto.builder()
            .id(statusListEntry.getStatusRegistryEntryId())
            .updatedAt(statusListEntry.getAuditMetadata().getLastModifiedAt())
            .createdAt(statusListEntry.getAuditMetadata().getCreatedAt())
            .build();
    }
}
