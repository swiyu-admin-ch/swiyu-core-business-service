/*
 * SPDX-FileCopyrightText: 2025 Swiss Confederation
 *
 * SPDX-License-Identifier: MIT
 */

package ch.admin.bj.swiyu.registry.identifier.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DatastoreStatus {
    ACTIVE("ACTIVE"),
    DEACTIVATED("DEACTIVATED"),
    DISABLED("DISABLED"),
    SETUP("SETUP");

    private final String value;
}
