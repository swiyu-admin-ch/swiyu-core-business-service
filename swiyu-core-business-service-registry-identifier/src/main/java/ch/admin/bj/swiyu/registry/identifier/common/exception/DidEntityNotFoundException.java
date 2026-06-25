/*
 * SPDX-FileCopyrightText: 2025 Swiss Confederation
 *
 * SPDX-License-Identifier: MIT
 */

package ch.admin.bj.swiyu.registry.identifier.common.exception;

public class DidEntityNotFoundException extends RuntimeException {

    public DidEntityNotFoundException(String id) {
        super("DidEntity with datastore id %s not found".formatted(id));
    }
}
