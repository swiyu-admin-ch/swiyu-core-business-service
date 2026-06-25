package ch.admin.bj.swiyu.core.business.modules.status.service;

import ch.admin.bj.swiyu.core.business.common.api.ApiObjectDto;
import ch.admin.bj.swiyu.core.business.common.api.ObjectLimitsDto;
import ch.admin.bj.swiyu.core.business.common.api.utils.PageableUtils;
import ch.admin.bj.swiyu.core.business.common.audit.AuditMapper;
import ch.admin.bj.swiyu.core.business.common.audit.AuditPublisher;
import ch.admin.bj.swiyu.core.business.common.exceptions.BusinessDataIntegrityViolationException;
import ch.admin.bj.swiyu.core.business.common.exceptions.ObjectCountLimitApiException;
import ch.admin.bj.swiyu.core.business.common.exceptions.ResourceNotFoundException;
import ch.admin.bj.swiyu.core.business.modules.status.api.StatusListEntryCreationDto;
import ch.admin.bj.swiyu.core.business.modules.status.api.StatusListEntryDto;
import ch.admin.bj.swiyu.core.business.modules.status.config.StatusListsLimitProperties;
import ch.admin.bj.swiyu.core.business.modules.status.domain.StatusListEntry;
import ch.admin.bj.swiyu.core.business.modules.status.domain.StatusListEntryRepository;
import ch.admin.bj.swiyu.registry.status.service.StatusListRegistryService;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@AllArgsConstructor
@Service
public class StatusListEntryService {

    private final StatusListEntryRepository statusListEntryRepository;
    private final StatusListRegistryService statusListRegistryService;
    private final StatusListValidator statusListValidator;
    private final StatusListsLimitProperties statusListsLimitProperties;
    private final AuditPublisher auditPublisher;

    @Transactional(readOnly = true)
    public ObjectLimitsDto getCurrentLimits(UUID businessEntityId) {
        return ObjectLimitsDto.builder()
            .relatesTo(ApiObjectDto.STATUSLIST_ENTRY)
            .currentCount(statusListEntryRepository.countByBusinessEntityId(businessEntityId))
            .maxCount(statusListsLimitProperties.defaultMaxCount())
            .build();
    }

    @Transactional
    public StatusListEntryCreationDto createStatusListEntry(UUID businessEntityId) throws ObjectCountLimitApiException {
        var currentLimits = getCurrentLimits(businessEntityId); // NOSONAR invoking transactional method is fine here
        if (currentLimits.currentCount() >= statusListsLimitProperties.defaultMaxCount()) {
            throw new ObjectCountLimitApiException(currentLimits.relatesTo().toString(), currentLimits.currentCount());
        }
        try {
            var registryEntry = statusListRegistryService.createDatastoreEntry();
            var savedEntity = statusListEntryRepository.saveAndFlush(
                new StatusListEntry(registryEntry.id(), businessEntityId)
            );
            auditCreated(savedEntity, businessEntityId);
            return StatusListEntryCreationDto.builder()
                .id(savedEntity.getStatusRegistryEntryId())
                .statusRegistryUrl(registryEntry.files().get("TokenStatusListJWT").readUri())
                .build();
        } catch (DataIntegrityViolationException e) {
            throw new BusinessDataIntegrityViolationException(
                "Status list entry creation failed. Please check if the business partner exists in this environment",
                e
            );
        }
    }

    @Transactional
    public void updateStatusListEntry(UUID businessEntityId, UUID statusRegistryEntryId, String statusListVc) {
        var entry = statusListEntryRepository
            .findByBusinessEntityIdAndStatusRegistryEntryId(businessEntityId, statusRegistryEntryId)
            .orElseThrow(() -> new ResourceNotFoundException("No such status list entry id is known."));
        statusListValidator.validateStatusListVc(entry, statusListVc);
        publish(entry, statusListVc, businessEntityId);
    }

    @Transactional
    public void updateStatusListEntryV2(UUID businessEntityId, UUID statusRegistryEntryId, String statusListVc) {
        var entry = statusListEntryRepository
            .findByBusinessEntityIdAndStatusRegistryEntryId(businessEntityId, statusRegistryEntryId)
            .orElseThrow(() -> new ResourceNotFoundException("No such status list entry id is known."));
        statusListValidator.validateStatusListVcV2(entry, statusListVc);
        publish(entry, statusListVc, businessEntityId);
    }

    private void publish(StatusListEntry entry, String statusListVc, UUID businessEntityId) {
        statusListRegistryService.publishStatusList(entry.getStatusRegistryEntryId(), statusListVc);
        entry.increaseUploadCount();
        auditChanged(entry, businessEntityId, statusListVc);
    }

    private void auditCreated(StatusListEntry entry, UUID businessEntityId) {
        auditPublisher.statusListEntryCreated(
            entry.getStatusRegistryEntryId().toString(),
            businessEntityId.toString(),
            AuditMapper.toAuditJson(entry)
        );
    }

    private void auditChanged(StatusListEntry entry, UUID businessEntityId, String statusListVc) {
        auditPublisher.statusListEntryChanged(
            entry.getStatusRegistryEntryId().toString(),
            String.valueOf(entry.getUploadCount()),
            businessEntityId.toString(),
            AuditMapper.toAuditJson(entry),
            statusListVc
        );
    }

    @Transactional(readOnly = true)
    public Page<StatusListEntryDto> getPagedByBusinessPartner(UUID businessEntityId, Pageable pageable) {
        return statusListEntryRepository
            .findAllByBusinessEntityId(
                businessEntityId,
                PageableUtils.toDbPageableFromUserPageable(StatusListEntryDto.class, StatusListEntry.class, pageable)
            )
            .map(StatusListEntryMapper::toStatusListEntryDto);
    }
}
