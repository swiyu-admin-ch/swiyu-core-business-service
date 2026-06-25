/*
 * SPDX-FileCopyrightText: 2025 Swiss Confederation
 *
 * SPDX-License-Identifier: MIT
 */

package ch.admin.bj.swiyu.registry.identifier.common.exception;

public class DidEntityNotReadyException extends RuntimeException {

    public DidEntityNotReadyException(String id) {
        super("DidEntity with id %s is not ready for processing.".formatted(id));
    }
}
