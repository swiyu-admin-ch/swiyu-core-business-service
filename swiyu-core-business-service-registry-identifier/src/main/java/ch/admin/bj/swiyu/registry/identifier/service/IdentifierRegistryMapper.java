/*
 * SPDX-FileCopyrightText: 2025 Swiss Confederation
 *
 * SPDX-License-Identifier: MIT
 */

package ch.admin.bj.swiyu.registry.identifier.service;

import ch.admin.bj.swiyu.registry.identifier.api.DatastoreEntityResponseDto;
import ch.admin.bj.swiyu.registry.identifier.api.DatastoreStatusDto;
import ch.admin.bj.swiyu.registry.identifier.api.DidEntityResponseDto;
import ch.admin.bj.swiyu.registry.identifier.domain.DatastoreStatus;
import ch.admin.bj.swiyu.registry.identifier.domain.DidEntity;
import ch.admin.bj.swiyu.registry.identifier.domain.IdentifierDatastoreEntity;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class IdentifierRegistryMapper {

    public static DatastoreEntityResponseDto toDatastoreEntityResponseDto(
        IdentifierDatastoreEntity entity,
        Map<String, DidEntityResponseDto> files
    ) {
        return new DatastoreEntityResponseDto(entity.getId(), toDatastoreStatusDto(entity.getStatus()), files);
    }

    private static DatastoreStatusDto toDatastoreStatusDto(DatastoreStatus source) {
        return switch (source) {
            case DatastoreStatus.SETUP -> DatastoreStatusDto.SETUP;
            case DatastoreStatus.ACTIVE -> DatastoreStatusDto.ACTIVE;
            case DatastoreStatus.DISABLED -> DatastoreStatusDto.DISABLED;
            case DatastoreStatus.DEACTIVATED -> DatastoreStatusDto.DEACTIVATED;
        };
    }

    public static DidEntityResponseDto toDidEntityResponseDto(DidEntity entity) {
        return new DidEntityResponseDto(entity.getReadUri(), entity.getContent() != null);
    }

    public static DidEntityResponseDto toNotConfiguredDidEntityResponseDto(String readUri) {
        return new DidEntityResponseDto(readUri, false);
    }
}
