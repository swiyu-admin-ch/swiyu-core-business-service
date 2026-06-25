/*
 * SPDX-FileCopyrightText: 2025 Swiss Confederation
 *
 * SPDX-License-Identifier: MIT
 */

package ch.admin.bj.swiyu.registry.identifier.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdentifierDatastoreEntityRepository extends JpaRepository<IdentifierDatastoreEntity, UUID> {}
