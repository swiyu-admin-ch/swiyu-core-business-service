/*
 * SPDX-FileCopyrightText: 2025 Swiss Confederation
 *
 * SPDX-License-Identifier: MIT
 */

package ch.admin.bj.swiyu.registry.identifier.service;

import static ch.admin.bj.swiyu.registry.identifier.service.IdentifierRegistryMapper.toDatastoreEntityResponseDto;
import static ch.admin.bj.swiyu.registry.identifier.service.IdentifierRegistryMapper.toNotConfiguredDidEntityResponseDto;
import static java.util.stream.Collectors.toMap;

import ch.admin.bj.swiyu.registry.identifier.IdentifierRegistryProperties;
import ch.admin.bj.swiyu.registry.identifier.api.DatastoreEntityResponseDto;
import ch.admin.bj.swiyu.registry.identifier.api.DidEntityResponseDto;
import ch.admin.bj.swiyu.registry.identifier.common.exception.DidEntityNotFoundException;
import ch.admin.bj.swiyu.registry.identifier.common.exception.DidEntityNotReadyException;
import ch.admin.bj.swiyu.registry.identifier.domain.*;
import jakarta.validation.Valid;
import java.text.MessageFormat;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@AllArgsConstructor
public class IdentifierRegistryService {

    private final DidEntityRepository didEntityRepository;
    private final IdentifierDatastoreEntityRepository identifierDatastoreEntityRepository;
    private final IdentifierRegistryProperties identifierRegistryProperties;

    @Transactional(transactionManager = "identifierRegistryTransactionManager")
    public DatastoreEntityResponseDto createDatastoreEntity() {
        log.debug("Creating new DatastoreEntity");
        var datastoreEntity = createEmptyDatastoreEntity();
        var didEntities = getAllDatastoreDidEntities(datastoreEntity.getId());
        return toDatastoreEntityResponseDto(datastoreEntity, didEntities);
    }

    @Transactional(readOnly = true, transactionManager = "identifierRegistryTransactionManager")
    public DatastoreEntityResponseDto getDatastoreEntity(@Valid UUID id) {
        log.debug("Looking up datastore entity for id: {}", id);
        var datastoreEntity = getDatastoreEntityById(id);
        var didEntities = this.getAllDatastoreDidEntities(id);
        return toDatastoreEntityResponseDto(datastoreEntity, didEntities);
    }

    @Transactional(transactionManager = "identifierRegistryTransactionManager")
    public DatastoreEntityResponseDto updateDidTdwEntry(@Valid UUID id, String content) {
        log.debug("Updating did:tdw entry for id: {}", id);
        var datastoreEntity = saveNewDidAndActivateDatastore(id, DidType.DID_TDW, content);
        var didEntities = getAllDatastoreDidEntities(id);
        return toDatastoreEntityResponseDto(datastoreEntity, didEntities);
    }

    @Transactional(readOnly = true, transactionManager = "identifierRegistryTransactionManager")
    public String getDidTdwFile(UUID datastoreEntityId) {
        var didEntity = this.didEntityRepository.findByBase_IdAndFileType(
            datastoreEntityId,
            DidType.DID_TDW
        ).orElseThrow(() -> new DidEntityNotFoundException(datastoreEntityId.toString()));
        return didEntity.getContent();
    }

    private IdentifierDatastoreEntity getDatastoreEntityById(UUID datastoreEntityId) {
        return this.identifierDatastoreEntityRepository.findById(datastoreEntityId).orElseThrow(() ->
            new DidEntityNotFoundException(datastoreEntityId.toString())
        );
    }

    private IdentifierDatastoreEntity createEmptyDatastoreEntity() {
        return this.identifierDatastoreEntityRepository.save(new IdentifierDatastoreEntity());
    }

    private IdentifierDatastoreEntity saveNewDidAndActivateDatastore(
        UUID datastoreEntityId,
        DidType didType,
        String content
    ) {
        var datastoreEntity = getDatastoreEntityById(datastoreEntityId);
        validateCanEdit(datastoreEntity);
        var existing = didEntityRepository.findByBase_IdAndFileType(datastoreEntity.getId(), didType);
        if (existing.isEmpty()) {
            var entity = createNewDid(datastoreEntity, didType, content);
            didEntityRepository.save(entity);
        } else {
            var entity = existing.get();
            entity.updateContent(content);
            didEntityRepository.save(entity);
        }
        return activateDatastoreEntity(datastoreEntity);
    }

    private IdentifierDatastoreEntity activateDatastoreEntity(IdentifierDatastoreEntity base) {
        base.changeStatus(DatastoreStatus.ACTIVE);
        return this.identifierDatastoreEntityRepository.save(base);
    }

    private DidEntity createNewDid(IdentifierDatastoreEntity base, DidType fileType, String content) {
        String readURI = createReadUri(base.getId(), fileType);
        return new DidEntity(base, fileType, content, readURI);
    }

    private String createReadUri(UUID datastoreEntityId, DidType fileType) {
        return switch (fileType) {
            case DID_WEB -> MessageFormat.format(this.identifierRegistryProperties.didWebTemplate(), datastoreEntityId);
            case DID_TDW -> MessageFormat.format(
                this.identifierRegistryProperties.didTdwRouteTemplate(),
                datastoreEntityId
            );
        };
    }

    private Map<String, DidEntityResponseDto> getAllDatastoreDidEntities(UUID datastoreEntityId) {
        var didEntities = this.didEntityRepository.findByBase_Id(datastoreEntityId);
        var result = didEntities
            .stream()
            .collect(toMap(e -> e.getFileType().name(), IdentifierRegistryMapper::toDidEntityResponseDto));
        // add did entities for missing types
        for (DidType type : DidType.values()) {
            if (result.get(type.name()) == null) {
                var readUri = createReadUri(datastoreEntityId, type);
                result.put(type.name(), toNotConfiguredDidEntityResponseDto(readUri));
            }
        }
        return result;
    }

    private static void validateCanEdit(IdentifierDatastoreEntity entry) throws DidEntityNotReadyException {
        if (entry.getStatus() == DatastoreStatus.DISABLED) throw new DidEntityNotReadyException(
            entry.getId().toString()
        );
    }
}
