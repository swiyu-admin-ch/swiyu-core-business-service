/*
 * SPDX-FileCopyrightText: 2025 Swiss Confederation
 *
 * SPDX-License-Identifier: MIT
 */

package ch.admin.bj.swiyu.registry.status;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.status-registry")
public record StatusRegistryProperties(
    /*
     * The format template to create a data read response for a status list entry.
     * Format specifiers:
     *   {0} -> The entry ID as UUID
     *   {1} -> The entry extension as string
     */
    @NotEmpty String dataUrlTemplate
) {}
