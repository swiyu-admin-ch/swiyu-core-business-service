package ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding;

import static ch.admin.bj.swiyu.core.business.common.trust.IdentityVerificationProgressStatusDto.*;

import ch.admin.bj.swiyu.core.business.common.TrustBusinessPartnerExpiryReminderTiming;
import ch.admin.bj.swiyu.core.business.common.trust.BusinessPartnerIdentitySnapshot;
import ch.admin.bj.swiyu.core.business.common.trust.IdentityVerificationProgress;
import ch.admin.bj.swiyu.core.business.common.trust.IdentityVerificationProgressProvider;
import ch.admin.bj.swiyu.core.business.common.trust.IdentityVerificationProgressStatusDto;
import ch.admin.bj.swiyu.core.business.modules.trust.config.TrustOnboardingSubmissionLimitProperties;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustOnboardingSubmission;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustOnboardingSubmissionRepository;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustOnboardingSubmissionType;
import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Computes the identity verification progress from the partner's {@code BusinessPartnerIdentity} and
 * its trust onboarding submission history.
 */
@AllArgsConstructor
@Component
public class DefaultIdentityVerificationProgressProvider implements IdentityVerificationProgressProvider {

    private final TrustOnboardingSubmissionRepository trustOnboardingSubmissionRepository;
    private final TrustOnboardingSubmissionLimitProperties trustOnboardingSubmissionLimitProperties;

    private static @NonNull IdentityVerificationProgressStatusDto computeSubmittedSubmissionStatus(boolean hasBpi) {
        return hasBpi ? RE_VERIFICATION_IN_PROGRESS : VERIFICATION_IN_PROGRESS;
    }

    private static @NonNull IdentityVerificationProgressStatusDto computeUnsubmittedSubmissionStatus(
        TrustOnboardingSubmission submission,
        boolean hasBpi
    ) {
        if (submission.getSubmittedAt() != null) return VERIFICATION_INFORMATION_REQUESTED_STARTED;
        return hasBpi ? RE_VERIFICATION_STARTED : VERIFICATION_STARTED;
    }

    @Override
    @Transactional(readOnly = true)
    public IdentityVerificationProgress verificationProgress(
        UUID businessPartnerId,
        @Nullable BusinessPartnerIdentitySnapshot identity
    ) {
        var submissions = trustOnboardingSubmissionRepository.findAllByPartnerIdOrderByInitiatedAtAsc(
            businessPartnerId
        );

        return computeVerificationProgress(identity, submissions);
    }

    /**
     * Compute the current trust state of a Business Partner based on its BusinessPartnerIdentity and its
     * different submissions.
     * This state does not consider the presence of Did for the business partner.
     *
     * @param identity    the partner's identity facts, or {@code null} if it has no identity yet
     * @param submissions the partner's submissions
     * @return identityVerificationProgress
     */
    // See DefaultIdentityVerificationProgressProviderTest.class for details cases
    IdentityVerificationProgress computeVerificationProgress(
        @Nullable BusinessPartnerIdentitySnapshot identity,
        List<TrustOnboardingSubmission> submissions
    ) {
        var hasBpi = identity != null;

        // Sort all submissions chronologically (oldest first)
        var sortedSubmissions = submissions
            .stream()
            .sorted(Comparator.comparing(TrustOnboardingSubmission::getInitiatedAt))
            .toList();

        // Walk all submissions in order, tracking the latest ongoing submission and the most
        // recent closed submission outcome (SUCCEEDED / REJECTED / expired). Most Recent closed submission states clear any
        // prior active submission; the most recent closed submission outcome decides when none is ongoing.
        var hasPreviouslySucceeded = hasBpi;
        TrustOnboardingSubmission lastClosedSubmissionSucceeded = null;
        var lastClosedSubmissionRejected = false;
        TrustOnboardingSubmission latestActive = null;
        for (var submission : sortedSubmissions) {
            switch (submission.getStatus()) {
                case SUCCEEDED -> {
                    hasPreviouslySucceeded = true;
                    lastClosedSubmissionSucceeded = submission;
                    lastClosedSubmissionRejected = false;
                    latestActive = null;
                }
                case REJECTED -> {
                    hasPreviouslySucceeded = false;
                    lastClosedSubmissionSucceeded = null;
                    lastClosedSubmissionRejected = true;
                    latestActive = null;
                }
                case UNSUBMITTED_TIMEOUT -> {
                    lastClosedSubmissionSucceeded = null;
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
            return IdentityVerificationProgress.of(hasBpi ? RE_VERIFICATION_REJECTED : VERIFICATION_REJECTED);
        }

        if (identity != null) {
            return computeProgressStateWhenHasBpi(identity, lastClosedSubmissionSucceeded);
        }

        // No BPI: only a prior SUCCEEDED submission (migration window, TMS BPI event not yet
        // received) counts as verified.
        return IdentityVerificationProgress.of(
            hasPreviouslySucceeded ? VERIFICATION_SUCCEEDED : VERIFICATION_NOT_STARTED
        );
    }

    private @NonNull IdentityVerificationProgress computeProgressStateWhenHasBpi(
        BusinessPartnerIdentitySnapshot identity,
        TrustOnboardingSubmission lastClosedSubmissionSucceeded
    ) {
        if (identity.active()) {
            if (isExpiringSoon(identity)) {
                return IdentityVerificationProgress.of(RE_VERIFICATION_REQUIRED);
            }
            // only a succeeded profile change or renewal is a re-verification, a registration is the first one
            if (
                lastClosedSubmissionSucceeded != null &&
                lastClosedSubmissionSucceeded.getType() != TrustOnboardingSubmissionType.REGISTRATION
            ) {
                return new IdentityVerificationProgress(RE_VERIFICATION_SUCCEEDED, identity.validUntil());
            }
            return IdentityVerificationProgress.of(VERIFICATION_SUCCEEDED);
        }
        // BPI DEACTIVATED: identity is gone, verification starts over.
        return IdentityVerificationProgress.of(VERIFICATION_NOT_STARTED);
    }

    private @NonNull IdentityVerificationProgress computeProgressStateWhenOngoingSubmission(
        TrustOnboardingSubmission latestActive,
        boolean hasBpi
    ) {
        var status = switch (latestActive.getStatus()) {
            case UNSUBMITTED -> computeUnsubmittedSubmissionStatus(latestActive, hasBpi);
            case SUBMITTED, RESUBMITTED -> computeSubmittedSubmissionStatus(hasBpi);
            case INFORMATION_REQUESTED -> VERIFICATION_INFORMATION_REQUESTED_REQUIRED;
            default -> throw new IllegalStateException("Unexpected active submission status");
        };
        return new IdentityVerificationProgress(status, computeMaxDate(latestActive));
    }

    private boolean isExpiringSoon(BusinessPartnerIdentitySnapshot identity) {
        var validUntil = identity.validUntil();
        return (
            validUntil != null &&
            validUntil.isBefore(Instant.now().plus(TrustBusinessPartnerExpiryReminderTiming.RE_VERIFICATION_WINDOW))
        );
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
}
