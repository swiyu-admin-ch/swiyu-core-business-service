/*
 * SPDX-FileCopyrightText: 2025 Swiss Confederation
 *
 * SPDX-License-Identifier: MIT
 */

package ch.admin.bj.swiyu.registry.status.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StatusListDatastoreEntityRepository extends JpaRepository<StatusListDatastoreEntity, UUID> {}
