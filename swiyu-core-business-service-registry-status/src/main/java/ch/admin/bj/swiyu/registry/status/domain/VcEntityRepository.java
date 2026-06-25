/*
 * SPDX-FileCopyrightText: 2025 Swiss Confederation
 *
 * SPDX-License-Identifier: MIT
 */

package ch.admin.bj.swiyu.registry.status.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VcEntityRepository extends JpaRepository<VcEntity, Long> {
    List<VcEntity> findByBase_Id(UUID baseId); // NOSONAR: Ignore warning for snake case variables because it is a spring data pattern
    Optional<VcEntity> findByBase_IdAndVcType(UUID baseId, VcType vcType); // NOSONAR: Ignore warning for snake case variables because it is a spring data pattern
}
