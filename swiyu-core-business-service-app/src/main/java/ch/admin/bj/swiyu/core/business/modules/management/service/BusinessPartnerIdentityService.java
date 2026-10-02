package ch.admin.bj.swiyu.core.business.modules.management.service;

import static ch.admin.bj.swiyu.core.business.modules.management.service.mapper.BusinessPartnerMapper.toBusinessPartnerIdentityStatus;

import ch.admin.bj.swiyu.core.business.common.exceptions.ResourceNotFoundException;
import ch.admin.bj.swiyu.core.business.modules.management.domain.BusinessPartnerIdentity;
import ch.admin.bj.swiyu.core.business.modules.management.domain.BusinessPartnerIdentityRepository;
import ch.admin.bj.swiyu.core.business.modules.management.domain.BusinessPartnerIdentityStatus;
import ch.admin.bj.swiyu.core.business.modules.management.domain.BusinessPartnerRepository;
import ch.admin.bj.swiyu.messagetype.ti.BusinessPartnerIdentityActivatedPayload;
import ch.admin.bj.swiyu.messagetype.ti.BusinessPartnerIdentityUpdatedPayload;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies the TMS-owned {@link BusinessPartnerIdentity} of a business partner.
 *
 * <p>Writes the identity through its own repository only and never loads or saves the
 * {@code BusinessEntity}, so that identity changes do not version-bump the partner and cannot collide
 * with concurrent partner updates (EID-7099).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessPartnerIdentityService {

    private final BusinessPartnerIdentityRepository businessPartnerIdentityRepository;
    private final BusinessPartnerRepository businessPartnerRepository;

    /**
     * Applies a new BusinessPartnerIdentity received from a TMS BPI activated event.
     */
    @Transactional
    public void applyActivatedBusinessPartnerIdentity(UUID partnerId, BusinessPartnerIdentityActivatedPayload event) {
        log.info("Applying BusinessPartnerIdentityActivatedPayload for partner '{}'", partnerId);
        upsert(
            partnerId,
            event.getValidUntil(),
            new ArrayList<>(event.getTrustedIdentifier()),
            BusinessPartnerIdentityStatus.ACTIVE,
            event.getLastActivated(),
            event.getUid(),
            new HashMap<>(event.getEntityName()),
            event.getVersion()
        );
    }

    /**
     * Applies a new BusinessPartnerIdentity received from a TMS BPI updated event.
     */
    @Transactional
    public void applyUpdatedBusinessPartnerIdentity(UUID partnerId, BusinessPartnerIdentityUpdatedPayload event) {
        log.info("Applying BusinessPartnerIdentityUpdatedPayload for partner '{}'", partnerId);
        upsert(
            partnerId,
            event.getValidUntil(),
            new ArrayList<>(event.getTrustedIdentifier()),
            toBusinessPartnerIdentityStatus(event.getStatus()),
            event.getLastActivated(),
            event.getUid(),
            new HashMap<>(event.getEntityName()),
            event.getVersion()
        );
    }

    /**
     * Sets the BPI status to DEACTIVATED, preserving all other BPI data.
     *
     * @return true if an identity existed and was deactivated, false if there was nothing to deactivate.
     *         The caller decides how to react - whether to log, to notify the partner, or to ignore it -
     *         because that depends on where the call comes from.
     */
    @Transactional
    public boolean deactivateBusinessPartnerIdentity(UUID partnerId, long tmsVersion) {
        log.info("Deactivating BusinessPartnerIdentity for partner '{}'", partnerId);
        var currentBpi = businessPartnerIdentityRepository.findById(partnerId);
        if (currentBpi.isEmpty()) {
            assertBusinessPartnerExists(partnerId);
            return false;
        }
        currentBpi.get().deactivate(tmsVersion);
        return true;
    }

    /**
     * Removes the identities of the given business partners with a single bulk delete that bypasses the
     * persistence context. Must be called before the partners are loaded in the same transaction: a
     * loaded partner still referencing a deleted identity cannot be flushed anymore.
     */
    @Transactional
    public void deleteBusinessPartnerIdentities(Collection<UUID> partnerIds) {
        businessPartnerIdentityRepository.deleteAllByIdInBatch(partnerIds);
    }

    @SuppressWarnings("java:S107") // mirrors the fields of BusinessPartnerIdentity
    private void upsert(
        UUID partnerId,
        Instant validUntil,
        List<String> trustedIdentifier,
        BusinessPartnerIdentityStatus status,
        Instant lastActivated,
        String uid,
        Map<String, String> entityName,
        Long tmsVersion
    ) {
        var bpi = businessPartnerIdentityRepository.findById(partnerId);
        if (bpi.isPresent()) {
            // an existing identity implies an existing partner (FK)
            bpi.get().apply(validUntil, trustedIdentifier, status, lastActivated, uid, entityName, tmsVersion);
        } else {
            assertBusinessPartnerExists(partnerId);
            businessPartnerIdentityRepository.save(
                new BusinessPartnerIdentity(
                    partnerId,
                    validUntil,
                    trustedIdentifier,
                    status,
                    lastActivated,
                    uid,
                    entityName,
                    tmsVersion
                )
            );
        }
    }

    private void assertBusinessPartnerExists(UUID partnerId) {
        if (!businessPartnerRepository.existsById(partnerId)) {
            throw new ResourceNotFoundException(String.format("Business partner with id '%s' not found.", partnerId));
        }
    }
}
