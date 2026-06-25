/*
 * SPDX-FileCopyrightText: 2025 Swiss Confederation
 *
 * SPDX-License-Identifier: MIT
 */

package ch.admin.bj.swiyu.registry.status.common.exception;

public class StatusListNotFoundException extends RuntimeException {

    public StatusListNotFoundException(String id) {
        super("StatusList with datastore id %s not found".formatted(id));
    }
}
