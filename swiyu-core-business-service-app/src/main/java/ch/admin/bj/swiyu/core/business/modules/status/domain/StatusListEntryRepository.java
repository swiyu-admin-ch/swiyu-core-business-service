package ch.admin.bj.swiyu.core.business.modules.status.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StatusListEntryRepository extends JpaRepository<StatusListEntry, UUID> {
    Optional<StatusListEntry> findByBusinessEntityIdAndStatusRegistryEntryId(
        UUID businessEntityId,
        UUID statusRegistryEntryId
    );

    Page<StatusListEntry> findAllByBusinessEntityId(UUID businessEntityId, Pageable pageable);

    long countByBusinessEntityId(UUID businessEntityId);
}
