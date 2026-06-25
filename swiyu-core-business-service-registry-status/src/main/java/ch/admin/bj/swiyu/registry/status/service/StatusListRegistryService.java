/*
 * SPDX-FileCopyrightText: 2025 Swiss Confederation
 *
 * SPDX-License-Identifier: MIT
 */

package ch.admin.bj.swiyu.registry.status.service;

import static ch.admin.bj.swiyu.registry.status.domain.VcType.TokenStatusListJWT;
import static ch.admin.bj.swiyu.registry.status.service.StatusListRegistryMapper.*;
import static java.util.stream.Collectors.toMap;

import ch.admin.bj.swiyu.registry.status.StatusRegistryProperties;
import ch.admin.bj.swiyu.registry.status.api.DatastoreEntityResponseDto;
import ch.admin.bj.swiyu.registry.status.api.VcEntityResponseDto;
import ch.admin.bj.swiyu.registry.status.common.exception.StatusListNotFoundException;
import ch.admin.bj.swiyu.registry.status.common.exception.StatusListNotReadyException;
import ch.admin.bj.swiyu.registry.status.domain.*;
import java.text.MessageFormat;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Slf4j
public class StatusListRegistryService {

    private final VcEntityRepository vcEntityRepository;
    private final StatusListDatastoreEntityRepository statusListDatastoreEntityRepository;
    private final StatusRegistryProperties statusRegistryProperties;

    @Transactional(transactionManager = "statusRegistryTransactionManager")
    public DatastoreEntityResponseDto createDatastoreEntry() {
        var datastoreEntity = statusListDatastoreEntityRepository.save(new StatusListDatastoreEntity());
        log.info("created datastore entry with id: {}", datastoreEntity.getId());
        return toDatastoreEntityResponseDto(datastoreEntity, getAllDatastoreFileEntity(datastoreEntity.getId()));
    }

    @Transactional(transactionManager = "statusRegistryTransactionManager")
    public DatastoreEntityResponseDto publishStatusList(UUID datastoreEntryId, String statusListVc) {
        var datastoreEntity = getDatastoreEntityById(datastoreEntryId);
        validateCanEdit(datastoreEntity);
        var vcType = TokenStatusListJWT;
        var vcPayload = extractVcPayload(statusListVc, vcType);
        var existing = vcEntityRepository.findByBase_IdAndVcType(datastoreEntity.getId(), vcType);
        if (existing.isEmpty()) {
            var vcEntity = new VcEntity(
                datastoreEntity,
                vcType,
                statusListVc,
                vcPayload,
                buildReadUri(datastoreEntryId)
            );
            vcEntityRepository.save(vcEntity);
        } else {
            var vcEntity = existing.get();
            vcEntity.updateVc(statusListVc, vcPayload);
            vcEntityRepository.save(vcEntity);
        }
        datastoreEntity.activate();
        statusListDatastoreEntityRepository.save(datastoreEntity);
        log.info("Published Status List for datastore entry with id: {}", datastoreEntity.getId());
        return toDatastoreEntityResponseDto(datastoreEntity, getAllDatastoreFileEntity(datastoreEntryId));
    }

    @Transactional(readOnly = true, transactionManager = "statusRegistryTransactionManager")
    public String getStatusListVc(UUID datastoreEntityId) {
        var vcEntity = this.vcEntityRepository.findByBase_IdAndVcType(datastoreEntityId, VcType.TokenStatusListJWT);
        if (vcEntity.isEmpty()) {
            throw new StatusListNotFoundException(datastoreEntityId.toString());
        }
        return vcEntity.get().getRawVc();
    }

    private Map<String, VcEntityResponseDto> getAllDatastoreFileEntity(UUID id) {
        var vcEntities = vcEntityRepository.findByBase_Id(id);
        var result = vcEntities
            .stream()
            .collect(toMap(e -> e.getVcType().name(), StatusListRegistryMapper::toVcEntityResponseDto));
        // add vc entities for missing types
        for (VcType type : VcType.values()) {
            if (result.get(type.name()) == null) {
                result.put(type.name(), toNotConfiguredVcEntityResponseDto(buildReadUri(id)));
            }
        }
        return result;
    }

    private String extractVcPayload(String encodedVc, VcType expectedType) {
        return switch (expectedType) {
            // NOSONAR: we want the code being ready for other types
            case TokenStatusListJWT -> extractJwtPayload(encodedVc);
        };
    }

    private String extractJwtPayload(String encodedVc) {
        return new String(Base64.getDecoder().decode(encodedVc.split("\\.")[1]));
    }

    private String buildReadUri(UUID datastoreEntityId) {
        return MessageFormat.format(statusRegistryProperties.dataUrlTemplate(), datastoreEntityId, "jwt");
    }

    private StatusListDatastoreEntity getDatastoreEntityById(UUID id) {
        return statusListDatastoreEntityRepository
            .findById(id)
            .orElseThrow(() -> new StatusListNotFoundException(id.toString()));
    }

    private static void validateCanEdit(StatusListDatastoreEntity entry) throws StatusListNotReadyException {
        if (entry.getStatus() == DatastoreStatus.DISABLED) {
            throw new StatusListNotReadyException(entry.getId().toString());
        }
    }
}
