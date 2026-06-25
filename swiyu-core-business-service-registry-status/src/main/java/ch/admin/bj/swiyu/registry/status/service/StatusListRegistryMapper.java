/*
 * SPDX-FileCopyrightText: 2025 Swiss Confederation
 *
 * SPDX-License-Identifier: MIT
 */

package ch.admin.bj.swiyu.registry.status.service;

import ch.admin.bj.swiyu.registry.status.api.DatastoreEntityResponseDto;
import ch.admin.bj.swiyu.registry.status.api.DatastoreStatusDto;
import ch.admin.bj.swiyu.registry.status.api.VcEntityResponseDto;
import ch.admin.bj.swiyu.registry.status.domain.DatastoreStatus;
import ch.admin.bj.swiyu.registry.status.domain.StatusListDatastoreEntity;
import ch.admin.bj.swiyu.registry.status.domain.VcEntity;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class StatusListRegistryMapper {

    public static DatastoreEntityResponseDto toDatastoreEntityResponseDto(
        StatusListDatastoreEntity entity,
        Map<String, VcEntityResponseDto> files
    ) {
        return new DatastoreEntityResponseDto(entity.getId(), toDatastoreStatusDto(entity.getStatus()), files);
    }

    public static VcEntityResponseDto toVcEntityResponseDto(VcEntity entity) {
        return new VcEntityResponseDto(entity.getRawVc() != null, entity.getReadUri());
    }

    public static VcEntityResponseDto toNotConfiguredVcEntityResponseDto(String readUri) {
        return new VcEntityResponseDto(false, readUri);
    }

    private static DatastoreStatusDto toDatastoreStatusDto(DatastoreStatus source) {
        if (source == null) {
            return null;
        }
        return switch (source) {
            case ACTIVE -> DatastoreStatusDto.ACTIVE;
            case DEACTIVATED -> DatastoreStatusDto.DEACTIVATED;
            case DISABLED -> DatastoreStatusDto.DISABLED;
            case SETUP -> DatastoreStatusDto.SETUP;
        };
    }
}
