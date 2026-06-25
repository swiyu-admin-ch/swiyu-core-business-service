/*
 * SPDX-FileCopyrightText: 2025 Swiss Confederation
 *
 * SPDX-License-Identifier: MIT
 */

package ch.admin.bj.swiyu.registry.identifier.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DidEntityRepository extends JpaRepository<DidEntity, Long> {
    List<DidEntity> findByBase_Id(UUID baseId); // NOSONAR naming based on convention

    Optional<DidEntity> findByBase_IdAndFileType(UUID baseId, DidType fileType); // NOSONAR naming based on convention
}
