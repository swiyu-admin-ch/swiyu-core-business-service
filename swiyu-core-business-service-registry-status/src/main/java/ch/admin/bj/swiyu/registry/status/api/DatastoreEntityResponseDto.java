/*
 * SPDX-FileCopyrightText: 2025 Swiss Confederation
 *
 * SPDX-License-Identifier: MIT
 */

package ch.admin.bj.swiyu.registry.status.api;

import java.util.Map;
import java.util.UUID;

public record DatastoreEntityResponseDto(UUID id, DatastoreStatusDto status, Map<String, VcEntityResponseDto> files) {}
