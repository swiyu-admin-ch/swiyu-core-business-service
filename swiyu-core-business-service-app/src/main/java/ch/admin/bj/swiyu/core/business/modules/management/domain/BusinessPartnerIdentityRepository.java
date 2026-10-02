package ch.admin.bj.swiyu.core.business.modules.management.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BusinessPartnerIdentityRepository extends JpaRepository<BusinessPartnerIdentity, UUID> {}
