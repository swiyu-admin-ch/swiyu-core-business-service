package ch.admin.bj.swiyu.core.business.modules.identifier.service;

import static ch.admin.bj.swiyu.core.business.common.api.utils.PageableUtils.toDbPageableFromUserPageable;

import ch.admin.bj.swiyu.core.business.common.api.ApiObjectDto;
import ch.admin.bj.swiyu.core.business.common.api.CountLimitDto;
import ch.admin.bj.swiyu.core.business.common.api.IdentifierUpdateRequestDto;
import ch.admin.bj.swiyu.core.business.common.audit.AuditMapper;
import ch.admin.bj.swiyu.core.business.common.audit.AuditPublisher;
import ch.admin.bj.swiyu.core.business.common.did.DidUtil;
import ch.admin.bj.swiyu.core.business.common.exceptions.ObjectCountLimitApiException;
import ch.admin.bj.swiyu.core.business.common.exceptions.ResourceNotFoundException;
import ch.admin.bj.swiyu.core.business.modules.identifier.api.IdentifierEntryDto;
import ch.admin.bj.swiyu.core.business.modules.identifier.api.IdentifierEntryFilterDto;
import ch.admin.bj.swiyu.core.business.modules.identifier.api.IdentifierEntryLimitsDto;
import ch.admin.bj.swiyu.core.business.modules.identifier.config.IdentifierLimitProperties;
import ch.admin.bj.swiyu.core.business.modules.identifier.domain.*;
import ch.admin.bj.swiyu.core.business.modules.identifier.exceptions.IdentifierValidationFailedException;
import ch.admin.bj.swiyu.registry.identifier.service.IdentifierRegistryService;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.querydsl.core.BooleanBuilder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@AllArgsConstructor
@Service
public class IdentifierEntryService {

    private final IdentifierRegistryService identifierRegistryService;
    private final IdentifierEntryRepository identifierEntryRepository;
    private final IdentifierValidator identifierValidator;
    private final IdentifierLimitProperties identifierLimitProperties;
    private final JsonMapper jsonMapper;
    private final IdentifierEntryMapper identifierEntryMapper;
    private final AuditPublisher auditPublisher;

    @Transactional(readOnly = true)
    public IdentifierEntryLimitsDto getLimits(@Valid @NotNull UUID businessEntityId) {
        return new IdentifierEntryLimitsDto(
            new CountLimitDto(
                ApiObjectDto.IDENTIFIER_ENTRY,
                identifierEntryRepository.countByBusinessEntityId(businessEntityId),
                identifierLimitProperties.defaultMaxCount()
            )
        );
    }

    @Transactional
    public IdentifierEntryDto createIdentifierEntry(UUID businessEntityId) throws ObjectCountLimitApiException {
        log.debug("Creating new identifier entry for businessEntityId {}", businessEntityId);
        var currentLimits = getLimits(businessEntityId); // NOSONAR invoking transactional method is fine here
        if (currentLimits.count().current() >= identifierLimitProperties.defaultMaxCount()) {
            throw new ObjectCountLimitApiException(
                currentLimits.count().relatesTo().toString(),
                currentLimits.count().current()
            );
        }

        var registryEntry = identifierRegistryService.createDatastoreEntity();
        var identifierEntry = identifierEntryRepository.save(new IdentifierEntry(registryEntry.id(), businessEntityId));
        auditCreated(identifierEntry, businessEntityId);
        return identifierEntryMapper.toIdentifierEntryDto(identifierEntry);
    }

    @Transactional
    public void updateIdentifierEntry(UUID businessEntityId, UUID identifierRegistryEntryId, String didLog) {
        log.debug(
            "Updating identifier entry with id {} for businessEntityId {}",
            identifierRegistryEntryId,
            businessEntityId
        );
        var entry = identifierEntryRepository
            .findByBusinessEntityIdAndId(businessEntityId, identifierRegistryEntryId)
            .orElseThrow(() -> new ResourceNotFoundException("No such identifier entry id is known"));

        identifierValidator.validateDidLog(entry, didLog);

        if (entry.getStatus() == IdentifierStatus.USER_DEACTIVATED) {
            throw new IdentifierValidationFailedException(
                "Status of identifier entry with id " + entry.getId() + " is 'USER_DEACTIVATED'",
                null
            );
        }

        identifierRegistryService.updateDidTdwEntry(entry.getId(), didLog);

        String did = DidUtil.getId(didLog, jsonMapper);

        if (isDidLogDeactivated(didLog)) {
            entry.updateDidAndDeactivate(did);
        } else if (entry.getUploadCount() == 0) {
            entry.updateDidAndActivate(did);
        }

        identifierEntryRepository.save(entry);
        auditChanged(entry, businessEntityId, didLog);
    }

    @Transactional(readOnly = true)
    public Page<IdentifierEntryDto> searchIdentifierEntries(IdentifierEntryFilterDto filter, Pageable pageable) {
        var q = QIdentifierEntry.identifierEntry;
        var where = new BooleanBuilder();
        if (filter.businessPartnerId() != null) {
            where.and(q.businessEntityId.eq(filter.businessPartnerId()));
        }
        if (Boolean.TRUE.equals(filter.activeOnly())) {
            where.and(q.status.eq(IdentifierStatus.INITIALIZED));
        }
        return identifierEntryRepository
            .findAll(where, toDbPageableFromUserPageable(IdentifierEntryDto.class, IdentifierEntry.class, pageable))
            .map(identifierEntryMapper::toIdentifierEntryDto);
    }

    @Transactional(readOnly = true)
    public IdentifierEntryDto getIdentifierEntry(
        @NotNull @Valid UUID businessEntityId,
        @NotNull @Valid UUID identifierEntryId
    ) {
        return this.identifierEntryRepository.findByBusinessEntityIdAndId(businessEntityId, identifierEntryId)
            .map(identifierEntryMapper::toIdentifierEntryDto)
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "No identifier entry found with businessEntityId " +
                        businessEntityId +
                        " and identifierId " +
                        identifierEntryId
                )
            );
    }

    @Transactional(readOnly = true)
    public boolean belongsIdentifierToBusinessPartner(UUID didEntryId, @NotNull UUID businessEntityId) {
        return identifierEntryRepository.existsByIdAndBusinessEntityId(didEntryId, businessEntityId);
    }

    @Transactional(readOnly = true)
    public boolean belongsDidToBusinessPartner(UUID businessEntityId, String did) {
        return identifierEntryRepository.existsByBusinessEntityIdAndDid(businessEntityId, did);
    }

    @Transactional
    public void updateIdentifierEntryDescription(
        UUID businessEntityId,
        UUID identifierId,
        IdentifierUpdateRequestDto updateRequestDto
    ) {
        var entry = identifierEntryRepository
            .findByBusinessEntityIdAndId(businessEntityId, identifierId)
            .orElseThrow(() -> new ResourceNotFoundException("No such identifier entry id is known"));
        entry.setDescription(updateRequestDto.description());
        identifierEntryRepository.save(entry);
        auditDescriptionChanged(entry, businessEntityId);
    }

    private void auditCreated(IdentifierEntry entry, UUID businessEntityId) {
        auditPublisher.identifierEntryCreated(
            entry.getId().toString(),
            businessEntityId.toString(),
            AuditMapper.toAuditJson(entry)
        );
    }

    private void auditChanged(IdentifierEntry entry, UUID businessEntityId, String didLog) {
        auditPublisher.identifierEntryChanged(
            entry.getId().toString(),
            String.valueOf(entry.getUploadCount()),
            businessEntityId.toString(),
            AuditMapper.toAuditJson(entry),
            didLog
        );
    }

    private void auditDescriptionChanged(IdentifierEntry entry, UUID businessEntityId) {
        auditPublisher.identifierEntryDescriptionChanged(
            entry.getId().toString(),
            String.valueOf(entry.getUploadCount()),
            businessEntityId.toString(),
            AuditMapper.toAuditJson(entry)
        );
    }

    private boolean isDidLogDeactivated(String didLog) {
        try {
            var didDoc = DidUtil.getDidDoc(didLog);
            return didDoc.getDeactivated();
        } catch (Exception e) {
            throw new IdentifierValidationFailedException("Error while trying to determine state of did log", e);
        }
    }
}
