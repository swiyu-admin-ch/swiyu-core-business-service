package ch.admin.bj.swiyu.core.business.modules.management.service;

import static ch.admin.bj.swiyu.core.business.common.domain.BusinessPartnerType.GOVERNMENTAL_INSTITUTION;
import static ch.admin.bj.swiyu.core.business.common.service.mapper.BusinessPartnerTypeMapper.toBusinessPartnerType;
import static ch.admin.bj.swiyu.core.business.common.service.mapper.BusinessPartnerTypeMapper.toBusinessPartnerTypeDto;
import static ch.admin.bj.swiyu.core.business.modules.management.service.mapper.BusinessPartnerMapper.toAddress;
import static ch.admin.bj.swiyu.core.business.modules.management.service.mapper.BusinessPartnerMapper.toContact;
import static org.springframework.util.StringUtils.hasText;

import ch.admin.bj.swiyu.core.business.common.api.BusinessPartnerTypeDto;
import ch.admin.bj.swiyu.core.business.common.api.ListItemDto;
import ch.admin.bj.swiyu.core.business.common.api.utils.PageableUtils;
import ch.admin.bj.swiyu.core.business.common.audit.AuditMapper;
import ch.admin.bj.swiyu.core.business.common.audit.AuditPublisher;
import ch.admin.bj.swiyu.core.business.common.domain.Address;
import ch.admin.bj.swiyu.core.business.common.domain.BusinessPartnerType;
import ch.admin.bj.swiyu.core.business.common.email.ExpiringPartnerIdentity;
import ch.admin.bj.swiyu.core.business.common.exceptions.BusinessDataIntegrityViolationException;
import ch.admin.bj.swiyu.core.business.common.exceptions.ResourceNotFoundException;
import ch.admin.bj.swiyu.core.business.common.service.LocalizedMapUtil;
import ch.admin.bj.swiyu.core.business.modules.identifier.api.IdentifierEntryFilterDto;
import ch.admin.bj.swiyu.core.business.modules.identifier.service.IdentifierEntryService;
import ch.admin.bj.swiyu.core.business.modules.management.api.*;
import ch.admin.bj.swiyu.core.business.modules.management.domain.BusinessEntity;
import ch.admin.bj.swiyu.core.business.modules.management.domain.BusinessPartnerIdentity;
import ch.admin.bj.swiyu.core.business.modules.management.domain.BusinessPartnerIdentityStatus;
import ch.admin.bj.swiyu.core.business.modules.management.domain.BusinessPartnerRepository;
import ch.admin.bj.swiyu.core.business.modules.management.domain.pams.PamsClient;
import ch.admin.bj.swiyu.core.business.modules.management.service.mapper.BusinessPartnerMapper;
import ch.admin.bj.swiyu.core.business.modules.trust.config.TrustOnboardingSubmissionLimitProperties;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustOnboardingSubmission;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustOnboardingSubmissionRepository;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@AllArgsConstructor
@Service
public class BusinessPartnerService {

    private static final String BUSINESS_PARTNER_WITH_ID_S_NOT_FOUND = "Business partner with id '%s' not found.";
    private static final Map<String, String> BUSINESS_PARTNER_SORT_FIELDS = Map.of(
        "name",
        "defaultEntityName",
        "entityName",
        "defaultEntityName"
    );
    private final BusinessPartnerRepository businessPartnerRepository;
    private final TrustOnboardingSubmissionRepository trustOnboardingSubmissionRepository;
    private final TrustOnboardingSubmissionLimitProperties trustOnboardingSubmissionLimitProperties;
    private final PamsClient pamsClient;
    private final IdentifierEntryService identifierEntryService;
    private final AuditPublisher auditPublisher;

    private static @NonNull Supplier<ResourceNotFoundException> throwNotFoundException(UUID id) {
        return () -> new ResourceNotFoundException(String.format(BUSINESS_PARTNER_WITH_ID_S_NOT_FOUND, id));
    }

    @Transactional(readOnly = true)
    public Page<BusinessEntityDto> getAllEntities(List<UUID> businessEntityIds, Pageable pageable) {
        return businessPartnerRepository
            .findAllByIdIn(businessEntityIds, toBusinessPartnerPageable(BusinessEntityDto.class, pageable))
            .map(this::toBusinessEntityDto);
    }

    @Transactional(readOnly = true)
    public Optional<BusinessEntityDto> getBusinessEntity(UUID id) {
        return businessPartnerRepository.findById(id).map(this::toBusinessEntityDto);
    }

    @SuppressWarnings("java:S1874") // allow V1 constructor of BusinessEntity, will be removed
    @Transactional
    public BusinessEntityDto createBusinessPartnerV1(CreateBusinessEntityDto request, String pamsUserAdminDirUid) {
        log.info(
            "Creating new business partner (V1) with name '{}' and contact email '{}'",
            request.name(),
            request.contactEmailAddress()
        );

        if (!hasText(pamsUserAdminDirUid)) {
            throw new IllegalArgumentException("Missing PAMS Admin User UID for creating Business partner");
        }
        var businessPartner = new BusinessEntity(
            UUID.randomUUID(),
            request.name(),
            request.contactEmailAddress(),
            toBusinessPartnerType(request.type())
        );
        businessPartner = businessPartnerRepository.saveAndFlush(businessPartner); // Needs flush for DB Data integrity
        auditPublisher.businessPartnerRegistered(
            businessPartner.getId().toString(),
            String.valueOf(businessPartner.getVersion()),
            AuditMapper.toAuditJson(businessPartner)
        );
        pamsClient.createBusinessPartner(businessPartner, pamsUserAdminDirUid);
        return toBusinessEntityDto(businessPartner);
    }

    @Transactional
    public BusinessPartnerDto createBusinessPartnerV2(CreatePartnerDto request, String pamsUserAdminDirUid) {
        log.info(
            "Creating new business partner (V2) with name '{}' and contact email '{}'",
            request.name(),
            request.contact().email()
        );

        if (!hasText(pamsUserAdminDirUid)) {
            throw new IllegalArgumentException("Missing PAMS Admin User UID for creating Business partner");
        }
        var businessPartner = new BusinessEntity(
            UUID.randomUUID(),
            LocalizedMapUtil.fromSingleName(request.name()),
            toContact(request.contact()),
            toBusinessPartnerType(request.partnerType()),
            toAddress(request.address()),
            request.uid()
        );
        // this will likely move to its own method once we have payment support. See feature: EIDARTFE-1297
        businessPartner.addPayedForDidSlots(1);
        businessPartner = businessPartnerRepository.save(businessPartner); // Needs flush for DB Data integrity
        identifierEntryService.createIdentifierEntry(businessPartner.getId());
        businessPartnerRepository.flush();
        auditPublisher.businessPartnerRegistered(
            businessPartner.getId().toString(),
            String.valueOf(businessPartner.getVersion()),
            AuditMapper.toAuditJson(businessPartner)
        );
        pamsClient.createBusinessPartner(businessPartner, pamsUserAdminDirUid);
        return toBusinessPartnerDto(businessPartner);
    }

    @SuppressWarnings("java:S1874") // Remove with EID-6624: uses deprecated UpdateBusinessEntityDto fields and getContactPhone()
    @Transactional
    public BusinessEntityDto updateBusinessEntity(
        UUID businessEntityId,
        UpdateBusinessEntityDto updateBusinessEntityDto
    ) {
        log.info("Updating business partner with id '{}'", businessEntityId);

        BusinessEntity businessPartner = businessPartnerRepository
            .findById(businessEntityId)
            .orElseThrow(throwNotFoundException(businessEntityId));
        businessPartner.update(
            businessPartner.getEntityName(),
            updateBusinessEntityDto.contactEmailAddress(),
            businessPartner.getAddress(),
            businessPartner.getUid(),
            businessPartner.getContactPhone()
        );

        // Only update PAMS if relevant data changed
        var previousDefaultName = LocalizedMapUtil.getDefaultValue(businessPartner.getEntityName());
        var newDefaultName = updateBusinessEntityDto.name();
        if (!previousDefaultName.equals(newDefaultName)) {
            businessPartner.setName(LocalizedMapUtil.fromSingleName(updateBusinessEntityDto.name()));
            pamsClient.updateBusinessPartner(businessPartner);
        }
        businessPartner = businessPartnerRepository.saveAndFlush(businessPartner);
        auditPublisher.businessPartnerUpdated(
            businessPartner.getId().toString(),
            String.valueOf(businessPartner.getVersion()),
            AuditMapper.toAuditJson(businessPartner)
        );
        return toBusinessEntityDto(businessPartner);
    }

    /**
     * Updates a business partner from the new PUT endpoint.
     * If the partner's BPI is ACTIVE (trusted), only address and contact are updated.
     * If not ACTIVE (not yet verified), name and uid can also be changed.
     */
    @Transactional
    public BusinessPartnerDto updateBusinessPartnerFromPortal(UUID businessPartnerId, BusinessPartnerUpdateDto dto) {
        log.info("Updating business partner with id '{}' from portal", businessPartnerId);

        BusinessEntity businessPartner = businessPartnerRepository
            .findById(businessPartnerId)
            .orElseThrow(throwNotFoundException(businessPartnerId));

        var contact = BusinessPartnerMapper.toContact(dto.contact());
        var address = toAddress(dto.address());

        if (businessPartner.isBusinessPartnerIdentityActive()) {
            // BPI is ACTIVE: only irrelevant info (address + contact) can be changed
            businessPartner.updateIrrelevantInfo(contact, address);
        } else {
            // BPI not yet ACTIVE: also allow name and uid updates
            var entityName = hasText(dto.name())
                ? LocalizedMapUtil.fromSingleName(dto.name())
                : businessPartner.getEntityName();
            businessPartner.updateAllEditableInfo(entityName, dto.uid(), contact, address);
        }

        pamsClient.updateBusinessPartner(businessPartner);
        businessPartner = businessPartnerRepository.saveAndFlush(businessPartner);
        auditPublisher.businessPartnerUpdated(
            businessPartner.getId().toString(),
            String.valueOf(businessPartner.getVersion()),
            AuditMapper.toAuditJson(businessPartner)
        );
        return toBusinessPartnerDto(businessPartner);
    }

    /**
     * Returns the identity verification progress for a business partner.
     *
     * <p>Delegates to {@link #computeVerificationProgress(BusinessEntity, List)} and additionally
     * enforces the rule that VERIFICATION_NOT_STARTED is returned instead of VERIFICATION_STARTED
     * when no active DID document exists yet (verification cannot be started without a DID).
     */
    @Transactional(readOnly = true)
    public IdentityVerificationProgressDto getVerificationProgress(UUID businessPartnerId) {
        var businessPartner = businessPartnerRepository
            .findById(businessPartnerId)
            .orElseThrow(throwNotFoundException(businessPartnerId));

        var submissions = trustOnboardingSubmissionRepository.findAllByPartnerIdOrderByInitiatedAtAsc(
            businessPartnerId
        );

        var progress = computeVerificationProgress(businessPartner, submissions);

        // If the computed state would be VERIFICATION_STARTED but no active DID exists yet,
        // keep it as VERIFICATION_NOT_STARTED — the portal cannot start verification without a DID.
        if (progress.status() == IdentityVerificationProgressStatusDto.VERIFICATION_STARTED) {
            boolean hasActiveDid = identifierEntryService
                .searchIdentifierEntries(
                    IdentifierEntryFilterDto.builder().businessPartnerId(businessPartnerId).activeOnly(true).build(),
                    PageRequest.ofSize(1)
                )
                .hasContent();
            if (!hasActiveDid) {
                return IdentityVerificationProgressDto.of(
                    IdentityVerificationProgressStatusDto.VERIFICATION_NOT_STARTED
                );
            }
        }

        return progress;
    }

    /**
     * Computes the {@link IdentityVerificationProgressDto} for a business partner purely from
     * its {@link BusinessPartnerIdentity} (TMS-owned) and the history of its
     * {@link TrustOnboardingSubmission}s. This value is never persisted.
     *
     * <h3>State derivation rules</h3>
     * <pre>
     * ── No BPI (partner never successfully onboarded) ─────────────────────────
     *   No active submission             → VERIFICATION_NOT_STARTED
     *   Latest active sub UNSUBMITTED    → VERIFICATION_STARTED
     *   Latest active sub SUBMITTED      → VERIFICATION_IN_PROGRESS
     *   Latest active sub INFO_REQUESTED → VERIFICATION_INFORMATION_REQUESTED
     *
     * ── BPI ACTIVE (partner is currently trusted, no new submission) ──────────
     *   No active submission             → VERIFICATION_SUCCEEDED
     *
     * ── BPI exists (ACTIVE or DEACTIVATED) + new submission in flight ─────────
     *   The existence of any BPI (regardless of ACTIVE/DEACTIVATED) signals a
     *   "previously successfully onboarded" partner. Any new submission started
     *   after that produces RE_* states:
     *   Latest active sub UNSUBMITTED    → RE_VERIFICATION_STARTED
     *   Latest active sub SUBMITTED      → RE_VERIFICATION_IN_PROGRESS
     *   Latest active sub INFO_REQUESTED → VERIFICATION_INFORMATION_REQUESTED
     *                                      (no RE_ prefix for this one — matches old model)
     *
     * ── BPI DEACTIVATED + no active submission ───────────────────────────────
     *   No active submission             → RE_VERIFICATION_REQUIRED
     * </pre>
     *
     * <h3>Note on VERIFICATION_NOT_STARTED vs VERIFICATION_STARTED</h3>
     * This method returns VERIFICATION_NOT_STARTED whenever no active submission exists and
     * the partner has no BPI. {@link #getVerificationProgress} additionally checks for the
     * presence of an active DID before returning VERIFICATION_STARTED, because the portal
     * cannot initiate verification without a DID document.
     *
     * <h3>RE_* state trigger — "previously succeeded" signal</h3>
     * A partner is considered "previously successfully onboarded" when either:
     * (a) a {@link BusinessPartnerIdentity} exists (set by a TMS BPI event), OR
     * (b) a {@link TrustOnboardingSubmission} with status {@code SUCCEEDED} exists in history.
     * Condition (b) covers the migration window before TMS sends BPI sync events (EID-6612).
     * Once all partners have a BPI seeded, (a) alone is sufficient.
     * When "previously succeeded", any in-flight submission produces RE_* states.
     *
     * <h3>SUCCEEDED submission + no active submission</h3>
     * When the latest terminal state is SUCCEEDED and there is no BPI yet
     * (migration window), the result is VERIFICATION_SUCCEEDED.
     * UNSUBMITTED_TIMEOUT after a SUCCEEDED reverts to VERIFIED (same as old model).
     */
    IdentityVerificationProgressDto computeVerificationProgress(
        BusinessEntity entity,
        List<TrustOnboardingSubmission> submissions
    ) {
        var bpi = entity.getBusinessPartnerIdentity();

        // Sort all submissions chronologically (oldest first)
        var sorted = submissions
            .stream()
            .sorted(Comparator.comparing(TrustOnboardingSubmission::getInitiatedAt))
            .toList();

        // Walk all submissions in order, exactly as the old aggregateTrustVerificationStatus did.
        // This produces the correct hasPreviouslySucceeded flag AND the correct final state from
        // terminal submissions. We then overlay the active-submission state at the end.
        var hasPreviouslySucceeded = bpi != null;
        TrustOnboardingSubmission latestActiveSubmission = null;
        for (var sub : sorted) {
            switch (sub.getStatus()) {
                case SUCCEEDED -> {
                    hasPreviouslySucceeded = true;
                    latestActiveSubmission = null; // terminal overrides any prior active
                }
                case REJECTED -> {
                    hasPreviouslySucceeded = false;
                    latestActiveSubmission = null;
                }
                case UNSUBMITTED_TIMEOUT -> latestActiveSubmission = null;
                case UNSUBMITTED, SUBMITTED, INFORMATION_REQUESTED -> latestActiveSubmission = sub;
            }
        }
        var latestActive = java.util.Optional.ofNullable(latestActiveSubmission);

        // BPI ACTIVE + no in-flight submission → fully verified
        if (bpi != null && bpi.getStatus() == BusinessPartnerIdentityStatus.ACTIVE && latestActive.isEmpty()) {
            return IdentityVerificationProgressDto.of(IdentityVerificationProgressStatusDto.VERIFICATION_SUCCEEDED);
        }

        // No BPI but previously succeeded (from submissions) + no active submission
        // → treat as verified (migration window: BPI event not yet received from TMS)
        if (bpi == null && hasPreviouslySucceeded && latestActive.isEmpty()) {
            return IdentityVerificationProgressDto.of(IdentityVerificationProgressStatusDto.VERIFICATION_SUCCEEDED);
        }

        // BPI DEACTIVATED + no active submission → re-verification required
        if (bpi != null && bpi.getStatus() == BusinessPartnerIdentityStatus.DEACTIVATED && latestActive.isEmpty()) {
            return IdentityVerificationProgressDto.of(IdentityVerificationProgressStatusDto.RE_VERIFICATION_REQUIRED);
        }

        // Previously succeeded + active in-flight submission → RE_* states
        if (hasPreviouslySucceeded && latestActive.isPresent()) {
            var status = switch (latestActive.get().getStatus()) {
                case UNSUBMITTED -> IdentityVerificationProgressStatusDto.RE_VERIFICATION_STARTED;
                case SUBMITTED -> IdentityVerificationProgressStatusDto.RE_VERIFICATION_IN_PROGRESS;
                case INFORMATION_REQUESTED -> IdentityVerificationProgressStatusDto.VERIFICATION_INFORMATION_REQUESTED;
                default -> IdentityVerificationProgressStatusDto.VERIFICATION_SUCCEEDED; // unreachable
            };
            return new IdentityVerificationProgressDto(status, computeMaxDate(status, latestActive.get()));
        }

        // No previous success — first-time verification path
        if (latestActive.isEmpty()) {
            return IdentityVerificationProgressDto.of(IdentityVerificationProgressStatusDto.VERIFICATION_NOT_STARTED);
        }
        var status = switch (latestActive.get().getStatus()) {
            case UNSUBMITTED -> IdentityVerificationProgressStatusDto.VERIFICATION_STARTED;
            case SUBMITTED -> IdentityVerificationProgressStatusDto.VERIFICATION_IN_PROGRESS;
            case INFORMATION_REQUESTED -> IdentityVerificationProgressStatusDto.VERIFICATION_INFORMATION_REQUESTED;
            default -> IdentityVerificationProgressStatusDto.VERIFICATION_NOT_STARTED; // unreachable
        };
        return new IdentityVerificationProgressDto(status, computeMaxDate(status, latestActive.get()));
    }

    /**
     * Maps an {@link IdentityVerificationProgressDto} to the legacy
     * {@link BusinessPartnerTrustStatusDto} that the portal still depends on
     * for its action-button visibility checks (until EID-6624 removes it).
     *
     * <pre>
     * VERIFICATION_NOT_STARTED         → NOT_VERIFIED
     * VERIFICATION_STARTED             → VERIFICATION_STARTED
     * VERIFICATION_IN_PROGRESS         → VERIFICATION_IN_PROGRESS
     * VERIFICATION_INFORMATION_REQUESTED → INFORMATION_REQUESTED
     * VERIFICATION_REJECTED            → NOT_VERIFIED  (placeholder, EID-6620)
     * VERIFICATION_SUCCEEDED           → VERIFIED
     * RE_VERIFICATION_REQUIRED         → NOT_VERIFIED
     * RE_VERIFICATION_STARTED          → RE_VERIFICATION_STARTED
     * RE_VERIFICATION_IN_PROGRESS      → RE_VERIFICATION_IN_PROGRESS
     * RE_VERIFICATION_REJECTED         → NOT_VERIFIED  (placeholder, EID-6620)
     * RE_VERIFICATION_SUCCEEDED        → VERIFIED      (placeholder, EID-6620)
     * </pre>
     */
    private BusinessPartnerTrustStatusDto toLegacyTrustStatus(IdentityVerificationProgressDto progress) {
        return switch (progress.status()) {
            case VERIFICATION_NOT_STARTED -> BusinessPartnerTrustStatusDto.NOT_VERIFIED;
            case VERIFICATION_STARTED -> BusinessPartnerTrustStatusDto.VERIFICATION_STARTED;
            case VERIFICATION_IN_PROGRESS -> BusinessPartnerTrustStatusDto.VERIFICATION_IN_PROGRESS;
            case VERIFICATION_INFORMATION_REQUESTED -> BusinessPartnerTrustStatusDto.INFORMATION_REQUESTED;
            case VERIFICATION_REJECTED -> BusinessPartnerTrustStatusDto.NOT_VERIFIED;
            case VERIFICATION_SUCCEEDED -> BusinessPartnerTrustStatusDto.VERIFIED;
            case RE_VERIFICATION_REQUIRED -> BusinessPartnerTrustStatusDto.NOT_VERIFIED;
            case RE_VERIFICATION_STARTED -> BusinessPartnerTrustStatusDto.RE_VERIFICATION_STARTED;
            case RE_VERIFICATION_IN_PROGRESS -> BusinessPartnerTrustStatusDto.RE_VERIFICATION_IN_PROGRESS;
            case RE_VERIFICATION_REJECTED -> BusinessPartnerTrustStatusDto.NOT_VERIFIED;
            case RE_VERIFICATION_SUCCEEDED -> BusinessPartnerTrustStatusDto.VERIFIED;
        };
    }

    /**
     * Computes the deadline for the given status based on the active submission's {@code initiatedAt}
     * plus the configured {@code max-age-in-unsubmitted} duration.
     *
     * <p>Applies to {@link IdentityVerificationProgressStatusDto#VERIFICATION_STARTED},
     * {@link IdentityVerificationProgressStatusDto#RE_VERIFICATION_STARTED}, and (temporarily)
     * {@link IdentityVerificationProgressStatusDto#VERIFICATION_INFORMATION_REQUESTED}.
     * For {@code VERIFICATION_INFORMATION_REQUESTED} this is an approximation; the correct
     * calculation will be introduced with EID-6376 based on the future {@code resubmitRequiredUntil}
     * property of {@link TrustOnboardingSubmission}.
     *
     * @return the computed deadline, or {@code null} if no deadline applies for the given status
     */
    private Instant computeMaxDate(
        IdentityVerificationProgressStatusDto status,
        TrustOnboardingSubmission activeSubmission
    ) {
        return switch (status) {
            case VERIFICATION_STARTED, RE_VERIFICATION_STARTED, VERIFICATION_INFORMATION_REQUESTED -> activeSubmission
                .getInitiatedAt()
                .plus(trustOnboardingSubmissionLimitProperties.maxAgeInUnsubmitted());
            default -> null;
        };
    }

    /**
     * Applies a new BusinessPartnerIdentity received from a TMS BPI activated or updated event.
     */
    @Transactional
    public void applyBusinessPartnerIdentity(UUID partnerId, BusinessPartnerIdentity bpi) {
        log.info("Applying BusinessPartnerIdentity event for partner '{}'", partnerId);
        BusinessEntity businessPartner = businessPartnerRepository
            .findById(partnerId)
            .orElseThrow(throwNotFoundException(partnerId));
        businessPartner.applyBusinessPartnerIdentityEvent(bpi);
        businessPartnerRepository.save(businessPartner);
    }

    /**
     * Sets the BPI status to DEACTIVATED, preserving all other BPI data.
     *
     * @return true if an identity existed and was deactivated, false if there was nothing to deactivate.
     *         The caller decides how to react - whether to log, to notify the partner, or to ignore it -
     *         because that depends on where the call comes from. This service also does not publish the
     *         email itself: the publisher resolves the contact person through this very service, and the
     *         two would form a bean cycle.
     */
    @Transactional
    public boolean deactivateBusinessPartnerIdentity(UUID partnerId, long tmsVersion) {
        log.info("Deactivating BusinessPartnerIdentity for partner '{}'", partnerId);
        BusinessEntity businessPartner = businessPartnerRepository
            .findById(partnerId)
            .orElseThrow(throwNotFoundException(partnerId));
        var currentBpi = businessPartner.getBusinessPartnerIdentity();
        var deactivated = currentBpi != null;
        if (deactivated) {
            businessPartner.applyBusinessPartnerIdentityEvent(currentBpi.withDeactivated(tmsVersion));
        }
        businessPartnerRepository.save(businessPartner);
        return deactivated;
    }

    /**
     * Email address of the designated contact person of the business partner in the service portal,
     * i.e. the recipient of the partner notification emails. Null if the partner has no contact.
     */
    @Transactional(readOnly = true)
    public String getContactEmail(UUID partnerId) {
        var businessPartner = businessPartnerRepository
            .findById(partnerId)
            .orElseThrow(throwNotFoundException(partnerId));
        return contactEmailOf(businessPartner);
    }

    /**
     * Partners whose active Trust Identity expires inside the given window, one page at a time.
     *
     * <p>For the nightly renewal reminders. Deliberately paged and projected: this runs over the whole
     * partner base, so it must not depend on the base being small.
     */
    @Transactional(readOnly = true)
    public Page<ExpiringPartnerIdentity> findIdentitiesExpiringBetween(
        Instant windowStart,
        Instant windowEnd,
        Pageable pageable
    ) {
        return businessPartnerRepository.findIdentitiesExpiringBetween(windowStart, windowEnd, pageable);
    }

    private static String contactEmailOf(BusinessEntity businessPartner) {
        return businessPartner.getContact() == null ? null : businessPartner.getContact().getEmail();
    }

    @Transactional
    public void updateBusinessPartner(
        UUID businessPartnerId,
        Map<String, String> entityName,
        Address address,
        String email,
        String uid,
        String phone,
        BusinessPartnerType type
    ) {
        log.info("Updating business partner with id '{}' from trust onboarding submission", businessPartnerId);
        BusinessEntity businessPartner = businessPartnerRepository
            .findById(businessPartnerId)
            .orElseThrow(throwNotFoundException(businessPartnerId));

        var previousDefaultName = LocalizedMapUtil.getDefaultValue(businessPartner.getEntityName());
        var newDefaultName = LocalizedMapUtil.getDefaultValue(entityName);
        var nameChanged = !previousDefaultName.equals(newDefaultName);

        businessPartner.update(entityName, email, address, uid, phone);
        businessPartner.setType(type);

        if (nameChanged) {
            pamsClient.updateBusinessPartner(businessPartner);
        }

        businessPartner = businessPartnerRepository.saveAndFlush(businessPartner);
        auditPublisher.businessPartnerUpdated(
            businessPartner.getId().toString(),
            String.valueOf(businessPartner.getVersion()),
            AuditMapper.toAuditJson(businessPartner)
        );
    }

    @SuppressWarnings({ "java:S1874" }) // Remove with EID-6656
    @Transactional
    public BusinessEntityDto updateBusinessEntityIsGovernment(UUID businessEntityId, boolean isGovernment) {
        log.warn("Updating business partner with id '{}' to be set as government.", businessEntityId);

        BusinessEntity businessPartner = businessPartnerRepository
            .findById(businessEntityId)
            .orElseThrow(throwNotFoundException(businessEntityId));
        if (
            (isGovernment && businessPartner.getType() == GOVERNMENTAL_INSTITUTION) ||
            (!isGovernment && businessPartner.getType() != GOVERNMENTAL_INSTITUTION)
        ) {
            return null;
        }

        businessPartner.setType(isGovernment ? GOVERNMENTAL_INSTITUTION : BusinessPartnerType.UNKNOWN);
        businessPartner = businessPartnerRepository.saveAndFlush(businessPartner);
        auditPublisher.businessPartnerUpdated(
            businessPartner.getId().toString(),
            String.valueOf(businessPartner.getVersion()),
            AuditMapper.toAuditJson(businessPartner)
        );
        return toBusinessEntityDto(businessPartner);
    }

    @Transactional
    public void deleteBusinessEntity(UUID businessEntityId) {
        log.info("Deleting business partner with id '{}'", businessEntityId);

        businessPartnerRepository.deleteById(businessEntityId);
        pamsClient.deleteBusinessPartner(businessEntityId.toString());
    }

    @Transactional(readOnly = true)
    public boolean isGovernmental(UUID partnerId) {
        if (partnerId == null) {
            return false;
        }
        var partnerType = lookupBusinessPartnerType(partnerId);
        return GOVERNMENTAL_INSTITUTION.equals(partnerType);
    }

    @Transactional(readOnly = true)
    public boolean isTrusted(UUID partnerId) {
        if (partnerId == null) {
            return false;
        }
        return businessPartnerRepository
            .findById(partnerId)
            .map(BusinessEntity::isBusinessPartnerIdentityActive)
            .orElse(false);
    }

    @SuppressWarnings({ "java:S1874" }) // Remove with EID-6656
    @Transactional(readOnly = true)
    public BusinessPartnerTypeDto getBusinessPartnerType(UUID partnerId) {
        if (partnerId == null) {
            return BusinessPartnerTypeDto.UNKNOWN;
        }
        var partnerType = lookupBusinessPartnerType(partnerId);
        return toBusinessPartnerTypeDto(partnerType);
    }

    @Transactional(readOnly = true)
    public Page<BusinessPartnerListItemDto> getAllPartnersById(List<UUID> businessPartnerIds, Pageable pageable) {
        return businessPartnerRepository
            .findAllByIdIn(businessPartnerIds, toBusinessPartnerPageable(BusinessPartnerListItemDto.class, pageable))
            .map(this::getBusinessPartnerListItemDto);
    }

    @Transactional(readOnly = true)
    public Page<BusinessPartnerListItemDto> getAllPartners(Pageable pageable) {
        return businessPartnerRepository
            .findAll(toBusinessPartnerPageable(BusinessPartnerListItemDto.class, pageable))
            .map(this::getBusinessPartnerListItemDto);
    }

    @Transactional(readOnly = true)
    public BusinessPartnerDto getBusinessPartner(UUID id) {
        var entity = businessPartnerRepository.findById(id).orElseThrow(throwNotFoundException(id));
        return toBusinessPartnerDto(entity);
    }

    @SuppressWarnings({ "java:S1874" }) // Remove with EID-6656
    private @NonNull BusinessPartnerType lookupBusinessPartnerType(UUID partnerId) {
        return businessPartnerRepository
            .findById(partnerId)
            .map(BusinessEntity::getType)
            .orElse(BusinessPartnerType.UNKNOWN);
    }

    private BusinessPartnerDto toBusinessPartnerDto(BusinessEntity entity) {
        var submissions = trustOnboardingSubmissionRepository.findAllByPartnerIdOrderByInitiatedAtAsc(entity.getId());
        var progress = computeVerificationProgress(entity, submissions);
        return BusinessPartnerMapper.toBusinessPartnerDto(
            entity,
            toLegacyTrustStatus(progress),
            progress.maxDateForStatus()
        );
    }

    private BusinessEntityDto toBusinessEntityDto(BusinessEntity businessPartner) {
        return BusinessPartnerMapper.toBusinessEntityDto(businessPartner);
    }

    private BusinessPartnerListItemDto getBusinessPartnerListItemDto(BusinessEntity entity) {
        var submissions = trustOnboardingSubmissionRepository.findAllByPartnerIdOrderByInitiatedAtAsc(entity.getId());
        var progress = computeVerificationProgress(entity, submissions);
        return BusinessPartnerMapper.toBusinessPartnerListItemDto(
            entity,
            toLegacyTrustStatus(progress),
            progress.maxDateForStatus()
        );
    }

    private Pageable toBusinessPartnerPageable(Class<? extends ListItemDto> dtoClass, Pageable pageable) {
        return PageableUtils.toDbPageableFromUserPageable(
            dtoClass,
            BusinessEntity.class,
            pageable,
            BUSINESS_PARTNER_SORT_FIELDS
        );
    }

    @Transactional(readOnly = true)
    public void validateBusinessPartnerExists(@Valid UUID businessEntityId)
        throws BusinessDataIntegrityViolationException {
        if (!businessPartnerRepository.existsById(businessEntityId)) {
            throw new BusinessDataIntegrityViolationException(
                "The business partner does not exist in this environment."
            );
        }
    }
}
