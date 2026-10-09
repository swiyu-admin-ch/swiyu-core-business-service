package ch.admin.bj.swiyu.core.business.modules.management.service;

import static ch.admin.bj.swiyu.core.business.common.domain.BusinessPartnerType.GOVERNMENTAL_INSTITUTION;
import static ch.admin.bj.swiyu.core.business.common.service.mapper.BusinessPartnerTypeMapper.toBusinessPartnerType;
import static ch.admin.bj.swiyu.core.business.common.service.mapper.BusinessPartnerTypeMapper.toBusinessPartnerTypeDto;
import static ch.admin.bj.swiyu.core.business.modules.management.api.IdentityVerificationProgressStatusDto.*;
import static ch.admin.bj.swiyu.core.business.modules.management.service.mapper.BusinessPartnerMapper.toAddress;
import static ch.admin.bj.swiyu.core.business.modules.management.service.mapper.BusinessPartnerMapper.toContact;
import static org.springframework.util.StringUtils.hasText;

import ch.admin.bj.swiyu.core.business.common.TrustBusinessPartnerExpiryReminderTiming;
import ch.admin.bj.swiyu.core.business.common.api.BusinessPartnerTypeDto;
import ch.admin.bj.swiyu.core.business.common.api.ListItemDto;
import ch.admin.bj.swiyu.core.business.common.api.utils.PageableUtils;
import ch.admin.bj.swiyu.core.business.common.audit.AuditMapper;
import ch.admin.bj.swiyu.core.business.common.audit.AuditPublisher;
import ch.admin.bj.swiyu.core.business.common.audit.AuditTrigger;
import ch.admin.bj.swiyu.core.business.common.domain.Address;
import ch.admin.bj.swiyu.core.business.common.domain.BusinessPartnerType;
import ch.admin.bj.swiyu.core.business.common.email.ExpiringPartnerIdentity;
import ch.admin.bj.swiyu.core.business.common.exceptions.BusinessDataIntegrityViolationException;
import ch.admin.bj.swiyu.core.business.common.exceptions.ExternalSystemException;
import ch.admin.bj.swiyu.core.business.common.exceptions.HardDeleteNotAllowedException;
import ch.admin.bj.swiyu.core.business.common.exceptions.ResourceNotFoundException;
import ch.admin.bj.swiyu.core.business.common.service.LocalizedMapUtil;
import ch.admin.bj.swiyu.core.business.modules.identifier.api.IdentifierEntryFilterDto;
import ch.admin.bj.swiyu.core.business.modules.identifier.service.IdentifierEntryService;
import ch.admin.bj.swiyu.core.business.modules.management.api.*;
import ch.admin.bj.swiyu.core.business.modules.management.domain.BusinessEntity;
import ch.admin.bj.swiyu.core.business.modules.management.domain.BusinessPartnerIdentity;
import ch.admin.bj.swiyu.core.business.modules.management.domain.BusinessPartnerIdentityRepository;
import ch.admin.bj.swiyu.core.business.modules.management.domain.BusinessPartnerIdentityStatus;
import ch.admin.bj.swiyu.core.business.modules.management.domain.BusinessPartnerRepository;
import ch.admin.bj.swiyu.core.business.modules.management.domain.pams.PamsClient;
import ch.admin.bj.swiyu.core.business.modules.management.service.mapper.BusinessPartnerMapper;
import ch.admin.bj.swiyu.core.business.modules.trust.config.TrustOnboardingSubmissionLimitProperties;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.event.TiBusinessPartnerHardDeletedEventBuilder;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.event.TiBusinessPartnerUpdatedEventBuilder;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustOnboardingSubmission;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustOnboardingSubmissionRepository;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.publisher.DomainEventPublisher;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@AllArgsConstructor
@Service
public class BusinessPartnerService {

    private static final String BUSINESS_PARTNER_WITH_ID_S_NOT_FOUND = "Business partner with id '%s' not found.";
    private static final int PUBLISH_PAGE_SIZE = 500;
    private static final Map<String, String> BUSINESS_PARTNER_SORT_FIELDS = Map.of("entityName", "defaultEntityName");
    private final BusinessPartnerRepository businessPartnerRepository;
    private final BusinessPartnerIdentityRepository businessPartnerIdentityRepository;
    private final TrustOnboardingSubmissionRepository trustOnboardingSubmissionRepository;
    private final TrustOnboardingSubmissionLimitProperties trustOnboardingSubmissionLimitProperties;
    private final PamsClient pamsClient;
    private final IdentifierEntryService identifierEntryService;
    private final AuditPublisher auditPublisher;
    private final DomainEventPublisher domainEventPublisher;

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
        publishBusinessPartnerUpdatedEventFor(businessPartner.getId());
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
        publishBusinessPartnerUpdatedEventFor(businessPartner.getId());
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
            // BPI is ACTIVE: name and uid are owned by TMS and must not be overwritten.
            // Only the irrelevant info (address + contact) may change, and only when provided.
            businessPartner.applyPartialUpdateFromPortal(null, null, contact, address);
        } else {
            // BPI not yet ACTIVE: also allow name and uid updates. Blank values are ignored so
            // PAMS is never updated with an empty name/uid.
            businessPartner.applyPartialUpdateFromPortal(dto.name(), dto.uid(), contact, address);
        }

        pamsClient.updateBusinessPartner(businessPartner);
        businessPartner = businessPartnerRepository.saveAndFlush(businessPartner);
        auditPublisher.businessPartnerUpdated(
            businessPartner.getId().toString(),
            String.valueOf(businessPartner.getVersion()),
            AuditMapper.toAuditJson(businessPartner)
        );
        publishBusinessPartnerUpdatedEventFor(businessPartner.getId());
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
                return IdentityVerificationProgressDto.of(VERIFICATION_NOT_STARTED);
            }
        }

        return progress;
    }

    /**
     * Compute the current trust state of Business Partner based on his BusinessPartnerIdentity, and it's different submissions.
     * This state does not consider the presence of Did for the business partner.
     * @param partner
     * @param submissions
     * @return identityVerificationProgress
     */
    // See BusinessPartnerServiceComputeVerificationProgressTest.class for details cases
    IdentityVerificationProgressDto computeVerificationProgress(
        BusinessEntity partner,
        List<TrustOnboardingSubmission> submissions
    ) {
        var bpi = partner.getBusinessPartnerIdentity();
        var hasBpi = bpi != null;

        // Sort all submissions chronologically (oldest first)
        var sortedSubmissions = submissions
            .stream()
            .sorted(Comparator.comparing(TrustOnboardingSubmission::getInitiatedAt))
            .toList();

        // Walk all submissions in order, tracking the latest ongoing submission and the most
        // recent closed submission outcome (SUCCEEDED / REJECTED / expired). Most Recent closed submission states clear any
        // prior active submission; the most recent closed submission outcome decides when none is ongoing.
        var hasPreviouslySucceeded = hasBpi;
        var lastClosedSubmissionSucceeded = false;
        var lastClosedSubmissionRejected = false;
        TrustOnboardingSubmission latestActive = null;
        for (var submission : sortedSubmissions) {
            switch (submission.getStatus()) {
                case SUCCEEDED -> {
                    hasPreviouslySucceeded = true;
                    lastClosedSubmissionSucceeded = true;
                    lastClosedSubmissionRejected = false;
                    latestActive = null;
                }
                case REJECTED -> {
                    hasPreviouslySucceeded = false;
                    lastClosedSubmissionSucceeded = false;
                    lastClosedSubmissionRejected = true;
                    latestActive = null;
                }
                case UNSUBMITTED_TIMEOUT -> {
                    lastClosedSubmissionSucceeded = false;
                    latestActive = null;
                }
                case UNSUBMITTED, SUBMITTED, INFORMATION_REQUESTED, RESUBMITTED -> {
                    latestActive = submission;
                    lastClosedSubmissionRejected = false;
                }
            }
        }

        // An ongoing submission decides the state on its own.
        if (latestActive != null) {
            return computeProgressStateWhenOngoingSubmission(latestActive, hasBpi);
        }

        // No ongoing submission → the most recent terminal outcome decides.
        if (lastClosedSubmissionRejected) {
            return IdentityVerificationProgressDto.of(hasBpi ? RE_VERIFICATION_REJECTED : VERIFICATION_REJECTED);
        }

        if (hasBpi) {
            return computeProgressStateWhenHasBpi(bpi, lastClosedSubmissionSucceeded);
        }

        // No BPI: only a prior SUCCEEDED submission (migration window, TMS BPI event not yet
        // received) counts as verified.
        return IdentityVerificationProgressDto.of(
            hasPreviouslySucceeded ? VERIFICATION_SUCCEEDED : VERIFICATION_NOT_STARTED
        );
    }

    private @NonNull IdentityVerificationProgressDto computeProgressStateWhenHasBpi(
        BusinessPartnerIdentity bpi,
        boolean lastTerminalSucceeded
    ) {
        if (bpi.getStatus() == BusinessPartnerIdentityStatus.ACTIVE) {
            if (isExpiringSoon(bpi)) {
                return IdentityVerificationProgressDto.of(RE_VERIFICATION_REQUIRED);
            }
            if (lastTerminalSucceeded) {
                return new IdentityVerificationProgressDto(RE_VERIFICATION_SUCCEEDED, bpi.getValidUntil());
            }
            return IdentityVerificationProgressDto.of(VERIFICATION_SUCCEEDED);
        }
        // BPI DEACTIVATED: identity is gone, verification starts over.
        return IdentityVerificationProgressDto.of(VERIFICATION_NOT_STARTED);
    }

    private @NonNull IdentityVerificationProgressDto computeProgressStateWhenOngoingSubmission(
        TrustOnboardingSubmission latestActive,
        boolean hasBpi
    ) {
        var status = switch (latestActive.getStatus()) {
            case UNSUBMITTED -> computeUnsubmittedSubmissionStatus(latestActive, hasBpi);
            case SUBMITTED, RESUBMITTED -> computeSubmittedSubmissionStatus(hasBpi);
            case INFORMATION_REQUESTED -> VERIFICATION_INFORMATION_REQUESTED_REQUIRED;
            default -> throw new IllegalStateException("Unexpected active submission status");
        };
        return new IdentityVerificationProgressDto(status, computeMaxDate(latestActive));
    }

    private static @NonNull IdentityVerificationProgressStatusDto computeSubmittedSubmissionStatus(boolean hasBpi) {
        return hasBpi
            ? IdentityVerificationProgressStatusDto.RE_VERIFICATION_IN_PROGRESS
            : IdentityVerificationProgressStatusDto.VERIFICATION_IN_PROGRESS;
    }

    private static @NonNull IdentityVerificationProgressStatusDto computeUnsubmittedSubmissionStatus(
        TrustOnboardingSubmission submission,
        boolean hasBpi
    ) {
        if (
            submission.getSubmittedAt() != null
        ) return IdentityVerificationProgressStatusDto.VERIFICATION_INFORMATION_REQUESTED_STARTED;
        return hasBpi
            ? IdentityVerificationProgressStatusDto.RE_VERIFICATION_STARTED
            : IdentityVerificationProgressStatusDto.VERIFICATION_STARTED;
    }

    private boolean isExpiringSoon(BusinessPartnerIdentity bpi) {
        var validUntil = bpi.getValidUntil();
        return (
            validUntil != null &&
            validUntil.isBefore(Instant.now().plus(TrustBusinessPartnerExpiryReminderTiming.RE_VERIFICATION_WINDOW))
        );
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
     * RE_VERIFICATION_REQUIRED         → VERIFIED
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
            case VERIFICATION_INFORMATION_REQUESTED_REQUIRED -> BusinessPartnerTrustStatusDto.INFORMATION_REQUESTED;
            case VERIFICATION_INFORMATION_REQUESTED_STARTED -> BusinessPartnerTrustStatusDto.VERIFICATION_IN_PROGRESS;
            case VERIFICATION_REJECTED -> BusinessPartnerTrustStatusDto.NOT_VERIFIED;
            case VERIFICATION_SUCCEEDED -> BusinessPartnerTrustStatusDto.VERIFIED;
            case RE_VERIFICATION_REQUIRED -> BusinessPartnerTrustStatusDto.VERIFIED;
            case RE_VERIFICATION_STARTED -> BusinessPartnerTrustStatusDto.RE_VERIFICATION_STARTED;
            case RE_VERIFICATION_IN_PROGRESS -> BusinessPartnerTrustStatusDto.RE_VERIFICATION_IN_PROGRESS;
            case RE_VERIFICATION_REJECTED -> BusinessPartnerTrustStatusDto.NOT_VERIFIED;
            case RE_VERIFICATION_SUCCEEDED -> BusinessPartnerTrustStatusDto.VERIFIED;
        };
    }

    /**
     * Computes the deadline for the currently ongoing submission based on its own status.
     *
     * <p>Applies to:
     * <ul>
     *   <li>{@code UNSUBMITTED} — the {@link IdentityVerificationProgressStatusDto#VERIFICATION_STARTED}
     *       / {@link IdentityVerificationProgressStatusDto#RE_VERIFICATION_STARTED} /
     *       {@link IdentityVerificationProgressStatusDto#VERIFICATION_INFORMATION_REQUESTED_STARTED}
     *       deadline, based on {@code initiatedAt + max-age-in-unsubmitted}.</li>
     *   <li>{@code RESUBMITTED} and {@code INFORMATION_REQUESTED} — the submission's
     *       {@code resubmitRequiredUntil} deadline (falling back to the unsubmitted-age
     *       approximation if it is not set).</li>
     *   <li>{@code SUBMITTED} — no deadline.</li>
     * </ul>
     *
     * @return the computed deadline, or {@code null} if no deadline applies for the given status
     */
    private Instant computeMaxDate(TrustOnboardingSubmission activeSubmission) {
        return switch (activeSubmission.getStatus()) {
            case UNSUBMITTED -> activeSubmission
                .getInitiatedAt()
                .plus(trustOnboardingSubmissionLimitProperties.maxAgeInUnsubmitted());
            case RESUBMITTED, INFORMATION_REQUESTED -> {
                var resubmitRequiredUntil = activeSubmission.getResubmitRequiredUntil();
                yield resubmitRequiredUntil != null
                    ? resubmitRequiredUntil
                    : activeSubmission
                          .getInitiatedAt()
                          .plus(trustOnboardingSubmissionLimitProperties.maxAgeInUnsubmitted());
            }
            default -> null;
        };
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
        businessPartner.changeType(type);

        if (nameChanged) {
            pamsClient.updateBusinessPartner(businessPartner);
        }

        businessPartner = businessPartnerRepository.saveAndFlush(businessPartner);
        auditPublisher.businessPartnerUpdated(
            businessPartner.getId().toString(),
            String.valueOf(businessPartner.getVersion()),
            AuditMapper.toAuditJson(businessPartner)
        );
        publishBusinessPartnerUpdatedEventFor(businessPartner.getId());
    }

    /**
     * Toggles the hard-delete safeguard of a business partner. While the flag is false,
     * CBS refuses every hard delete of the partner regardless of the caller. Arming a governmental
     * institution is refused by the domain ({@link BusinessEntity#allowHardDelete()}).
     * Every change is audited and published as TiBusinessPartnerUpdatedEvent.
     */
    @Transactional
    public BusinessPartnerDto changeHardDeleteAllowed(UUID businessPartnerId, boolean hardDeleteAllowed) {
        log.info("Setting hardDeleteAllowed={} for business partner '{}'", hardDeleteAllowed, businessPartnerId);
        BusinessEntity businessPartner = businessPartnerRepository
            .findById(businessPartnerId)
            .orElseThrow(throwNotFoundException(businessPartnerId));

        if (hardDeleteAllowed) {
            businessPartner.allowHardDelete();
        } else {
            businessPartner.unallowHardDelete();
        }

        businessPartner = businessPartnerRepository.saveAndFlush(businessPartner);
        auditPublisher.businessPartnerUpdated(
            businessPartner.getId().toString(),
            String.valueOf(businessPartner.getVersion()),
            AuditMapper.toAuditJson(businessPartner)
        );
        publishBusinessPartnerUpdatedEventFor(businessPartner.getId());
        return toBusinessPartnerDto(businessPartner);
    }

    @Transactional(readOnly = true)
    public boolean businessPartnerExists(UUID businessPartnerId) {
        return businessPartnerRepository.existsById(businessPartnerId);
    }

    /**
     * Refuses the deletion unless ops has armed the partner. The single place that decides this - every
     * step of the hard delete calls it rather than reading the flag itself, so arming cannot be bypassed
     * by entering the sequence somewhere in the middle.
     */
    @Transactional(readOnly = true)
    public void validateHardDeleteAllowed(UUID businessPartnerId) {
        validateHardDeleteAllowed(loadBusinessPartner(businessPartnerId));
    }

    private static void validateHardDeleteAllowed(BusinessEntity businessPartner) {
        if (!businessPartner.isHardDeleteAllowed()) {
            throw new HardDeleteNotAllowedException(businessPartner.getId());
        }
    }

    private BusinessEntity loadBusinessPartner(UUID businessPartnerId) {
        return businessPartnerRepository
            .findById(businessPartnerId)
            .orElseThrow(throwNotFoundException(businessPartnerId));
    }

    /** Treats 404 as done, so a retry of a partially completed hard delete gets through. */
    public void deleteFromPams(UUID businessPartnerId) {
        // Loads the partner itself instead of calling the transactional validateHardDeleteAllowed(UUID): no
        // transaction must stay open across the PAMS call, and the in-class call would bypass the proxy anyway.
        validateHardDeleteAllowed(loadBusinessPartner(businessPartnerId));
        try {
            pamsClient.deleteBusinessPartner(businessPartnerId.toString());
        } catch (ExternalSystemException e) {
            if (!HttpStatus.NOT_FOUND.isSameCodeAs(e.getHttpStatusCode())) {
                throw e;
            }
            log.info("Business partner '{}' is unknown to PAMS, treating as deleted", businessPartnerId);
        }
    }

    /**
     * Last step of the hard delete - every other table of the partner must be empty by now. Audit,
     * deletion and the event for TMS share one transaction, so the event only goes out if the rows are
     * really gone. The safeguard is re-checked because this is the point of no return.
     */
    @Transactional
    public void hardDeleteBusinessPartner(UUID businessPartnerId, AuditTrigger trigger) {
        var businessPartner = loadBusinessPartner(businessPartnerId);
        validateHardDeleteAllowed(businessPartner);
        log.info("Hard deleting business partner '{}'", businessPartnerId);

        auditPublisher.businessPartnerDeleted(
            businessPartnerId.toString(),
            String.valueOf(businessPartner.getVersion()),
            AuditMapper.toAuditJson(businessPartner),
            trigger
        );

        businessPartnerIdentityRepository.deleteById(businessPartnerId);
        businessPartnerRepository.delete(businessPartner);
        domainEventPublisher.publishTiBusinessPartnerHardDeletedEvent(
            TiBusinessPartnerHardDeletedEventBuilder.create().businessPartnerId(businessPartnerId).build()
        );
    }

    /**
     * Publishes the TiBusinessPartnerUpdatedEvent for one partner (also the DevOps sync trigger,
     * EID-6988). No state change, idempotent. Must not be invoked for BusinessPartnerIdentity
     * changes - those originate from TMS and would be echoed back.
     */
    @Transactional // required: the outbox publisher demands an open transaction (MANDATORY)
    public void publishBusinessPartnerUpdatedEvent(UUID businessPartnerId) {
        log.info("Publishing TiBusinessPartnerUpdatedEvent for partner '{}'", businessPartnerId);
        if (!businessPartnerRepository.existsById(businessPartnerId)) {
            throw throwNotFoundException(businessPartnerId).get();
        }
        publishBusinessPartnerUpdatedEventFor(businessPartnerId);
    }

    /**
     * Publishes the TiBusinessPartnerUpdatedEvent for all partners (DevOps "sync all", EID-6988).
     * Pages over the partner ids only, so the whole base is never loaded into the first-level cache.
     */
    @Transactional
    public void publishAllBusinessPartnerUpdatedEvents() {
        log.info("Publishing TiBusinessPartnerUpdatedEvent for all partners");
        var pageable = Pageable.ofSize(PUBLISH_PAGE_SIZE);
        Page<UUID> ids;
        do {
            ids = businessPartnerRepository.findAllIds(pageable);
            ids.forEach(this::publishBusinessPartnerUpdatedEventFor);
            pageable = pageable.next();
        } while (ids.hasNext());
    }

    @Transactional(readOnly = true)
    public long count() {
        return businessPartnerRepository.count();
    }

    @Transactional(readOnly = true)
    public long countByType(BusinessPartnerTypeDto type) {
        return businessPartnerRepository.countByType(toBusinessPartnerType(type));
    }

    @Transactional(readOnly = true)
    public boolean isGovernmental(UUID partnerId) {
        if (partnerId == null) {
            throw new IllegalArgumentException("BusinessPartnerId cannot be null");
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

    @Transactional(readOnly = true)
    public BusinessPartnerTypeDto getBusinessPartnerType(UUID partnerId) {
        if (partnerId == null) {
            throw new IllegalArgumentException("BusinessPartnerId cannot be null");
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

    @Transactional(readOnly = true)
    public void validateBusinessPartnerExists(@Valid UUID businessEntityId)
        throws BusinessDataIntegrityViolationException {
        if (!businessPartnerRepository.existsById(businessEntityId)) {
            throw new BusinessDataIntegrityViolationException(
                "The business partner does not exist in this environment."
            );
        }
    }

    /**
     * Must only be called for creates/updates of the partner itself - never for
     * BusinessPartnerIdentity changes, which originate from TMS and would be echoed back.
     */
    private void publishBusinessPartnerUpdatedEventFor(UUID businessPartnerId) {
        domainEventPublisher.publishTiBusinessPartnerUpdatedEvent(
            TiBusinessPartnerUpdatedEventBuilder.create().businessPartnerId(businessPartnerId).build()
        );
    }

    private @NonNull BusinessPartnerType lookupBusinessPartnerType(UUID partnerId) {
        return businessPartnerRepository
            .findById(partnerId)
            .map(BusinessEntity::getType)
            .orElseThrow(throwNotFoundException(partnerId));
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
}
