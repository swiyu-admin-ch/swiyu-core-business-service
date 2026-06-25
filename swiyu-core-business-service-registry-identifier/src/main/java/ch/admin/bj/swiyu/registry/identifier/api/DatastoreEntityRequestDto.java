/*
 * SPDX-FileCopyrightText: 2025 Swiss Confederation
 *
 * SPDX-License-Identifier: MIT
 */

package ch.admin.bj.swiyu.registry.identifier.api;

import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DatastoreEntityRequestDto {

    private DatastoreStatusDto status;

    public Optional<DatastoreStatusDto> getStatus() {
        return Optional.ofNullable(status);
    }
}
