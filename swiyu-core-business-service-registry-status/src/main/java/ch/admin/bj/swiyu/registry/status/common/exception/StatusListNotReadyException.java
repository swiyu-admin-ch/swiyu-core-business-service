/*
 * SPDX-FileCopyrightText: 2025 Swiss Confederation
 *
 * SPDX-License-Identifier: MIT
 */

package ch.admin.bj.swiyu.registry.status.common.exception;

public class StatusListNotReadyException extends RuntimeException {

    public StatusListNotReadyException(String id) {
        super("StatusList with datastore id %s is not ready for processing.".formatted(id));
    }
}
