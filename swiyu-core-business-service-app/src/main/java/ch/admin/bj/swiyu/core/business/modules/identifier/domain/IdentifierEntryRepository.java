package ch.admin.bj.swiyu.core.business.modules.identifier.domain;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface IdentifierEntryRepository
    extends JpaRepository<IdentifierEntry, UUID>, QuerydslPredicateExecutor<IdentifierEntry>
{
    Optional<IdentifierEntry> findByBusinessEntityIdAndId(UUID businessEntityId, UUID identifierId);

    long countByBusinessEntityId(UUID businessEntityId);

    long countByStatus(IdentifierStatus status);

    boolean existsByIdAndBusinessEntityId(UUID didEntryId, @NotNull UUID businessEntityId);

    boolean existsByBusinessEntityIdAndDid(UUID businessEntityId, String did);

    List<IdentifierEntry> findAllByBusinessEntityId(UUID businessEntityId);
}
