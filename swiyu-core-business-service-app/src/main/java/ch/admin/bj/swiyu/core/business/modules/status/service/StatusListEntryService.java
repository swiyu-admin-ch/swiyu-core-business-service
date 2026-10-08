package ch.admin.bj.swiyu.core.business.modules.status.service;

import ch.admin.bj.swiyu.core.business.common.api.ApiObjectDto;
import ch.admin.bj.swiyu.core.business.common.api.CountLimitDto;
import ch.admin.bj.swiyu.core.business.common.api.utils.PageableUtils;
import ch.admin.bj.swiyu.core.business.common.audit.AuditMapper;
import ch.admin.bj.swiyu.core.business.common.audit.AuditPublisher;
import ch.admin.bj.swiyu.core.business.common.audit.AuditTrigger;
import ch.admin.bj.swiyu.core.business.common.exceptions.BusinessDataIntegrityViolationException;
import ch.admin.bj.swiyu.core.business.common.exceptions.ObjectCountLimitApiException;
import ch.admin.bj.swiyu.core.business.common.exceptions.ResourceNotFoundException;
import ch.admin.bj.swiyu.core.business.modules.management.service.BusinessPartnerService;
import ch.admin.bj.swiyu.core.business.modules.status.api.StatusListEntryCreationDto;
import ch.admin.bj.swiyu.core.business.modules.status.api.StatusListEntryDto;
import ch.admin.bj.swiyu.core.business.modules.status.api.StatusListEntryLimitsDto;
import ch.admin.bj.swiyu.core.business.modules.status.config.StatusListsLimitProperties;
import ch.admin.bj.swiyu.core.business.modules.status.domain.StatusListEntry;
import ch.admin.bj.swiyu.core.business.modules.status.domain.StatusListEntryRepository;
import ch.admin.bj.swiyu.registry.status.common.exception.StatusListNotFoundException;
import ch.admin.bj.swiyu.registry.status.service.StatusListRegistryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@AllArgsConstructor
@Service
public class StatusListEntryService {

    private final StatusListEntryRepository statusListEntryRepository;
    private final StatusListRegistryService statusListRegistryService;
    private final StatusListValidator statusListValidator;
    private final StatusListsLimitProperties statusListsLimitProperties;
    private final AuditPublisher auditPublisher;
    private final BusinessPartnerService businessPartnerService;

    @Transactional(readOnly = true)
    public long count() {
        return statusListEntryRepository.count();
    }

    @Transactional(readOnly = true)
    public StatusListEntryLimitsDto getLimits(@Valid @NotNull UUID businessEntityId) {
        return new StatusListEntryLimitsDto(
            new CountLimitDto(
                ApiObjectDto.STATUSLIST_ENTRY,
                statusListEntryRepository.countByBusinessEntityId(businessEntityId),
                statusListsLimitProperties.defaultMaxCount()
            )
        );
    }

    @Transactional
    public StatusListEntryCreationDto createStatusListEntry(UUID businessEntityId) throws ObjectCountLimitApiException {
        businessPartnerService.validateBusinessPartnerExists(businessEntityId);
        var currentLimits = getLimits(businessEntityId); // NOSONAR invoking transactional method is fine here
        if (currentLimits.count().current() >= statusListsLimitProperties.defaultMaxCount()) {
            throw new ObjectCountLimitApiException(
                currentLimits.count().relatesTo().toString(),
                currentLimits.count().current()
            );
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
            throw new BusinessDataIntegrityViolationException("Status list entry creation failed.", e);
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

        String oldStatusList = null;
        try {
            oldStatusList = statusListRegistryService.getStatusListVc(entry.getStatusRegistryEntryId());
        } catch (StatusListNotFoundException e) {
            // Nothing to do, this is a newly uploaded statuslist
        }
        statusListValidator.validateStatusListVcV2(entry, statusListVc, oldStatusList);
        publish(entry, statusListVc, businessEntityId);
    }

    private void publish(StatusListEntry entry, String statusListVc, UUID businessEntityId) {
        statusListRegistryService.publishStatusList(entry.getStatusRegistryEntryId(), statusListVc);
        entry.increaseUploadCount();
        auditChanged(entry, businessEntityId, statusListVc);
    }

    /**
     * Registry rows first, core rows second: the core entries are the only pointer into the registry DB,
     * so losing them first would orphan the registry data. Separate transaction managers, so the registry
     * delete commits on its own when it returns.
     */
    @Transactional
    public void hardDeleteByPartnerId(UUID businessEntityId, AuditTrigger trigger) {
        var entries = statusListEntryRepository.findAllByBusinessEntityId(businessEntityId);
        log.info("Hard deleting {} status list entries of business partner '{}'", entries.size(), businessEntityId);
        for (var entry : entries) {
            auditDeleted(entry, businessEntityId, statusListVcOf(entry), trigger);
        }
        statusListRegistryService.hardDelete(entries.stream().map(StatusListEntry::getStatusRegistryEntryId).toList());
        statusListEntryRepository.deleteAll(entries);
    }

    /** {@code null} if a previous, partially completed run already removed it. */
    private String statusListVcOf(StatusListEntry entry) {
        var statusListVc = statusListRegistryService.findStatusListVc(entry.getStatusRegistryEntryId());
        if (statusListVc.isEmpty()) {
            log.warn(
                "No status list found for entry '{}', auditing its deletion without the status list",
                entry.getStatusRegistryEntryId()
            );
        }
        return statusListVc.orElse(null);
    }

    private void auditDeleted(StatusListEntry entry, UUID businessEntityId, String statusListVc, AuditTrigger trigger) {
        auditPublisher.statusListEntryDeleted(
            entry.getStatusRegistryEntryId().toString(),
            String.valueOf(entry.getUploadCount()),
            businessEntityId.toString(),
            AuditMapper.toAuditJson(entry),
            statusListVc,
            trigger
        );
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
