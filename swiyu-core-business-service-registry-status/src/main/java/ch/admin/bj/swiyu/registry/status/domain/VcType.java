/*
 * SPDX-FileCopyrightText: 2025 Swiss Confederation
 *
 * SPDX-License-Identifier: MIT
 */

package ch.admin.bj.swiyu.registry.status.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum VcType {
    TokenStatusListJWT, // NOSONAR: fixing this case-naming-tech-debt is quite expensive (flyway migration or custom converter, sync with data repo)
}
