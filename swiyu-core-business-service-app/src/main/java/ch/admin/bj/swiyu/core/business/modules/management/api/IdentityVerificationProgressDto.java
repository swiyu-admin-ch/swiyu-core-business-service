package ch.admin.bj.swiyu.core.business.modules.management.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * Represents the current progress of identity verification for a business partner,
 * together with an optional deadline by which the current verification step must be completed.
 *
 * <p>The {@link #status()} field carries the computed progress state (see
 * {@link IdentityVerificationProgressStatusDto} for state derivation rules).
 *
 * <p>The {@link #maxDateForStatus()} field is non-null for states that have a time-bound deadline:
 * {@link IdentityVerificationProgressStatusDto#VERIFICATION_STARTED},
 * {@link IdentityVerificationProgressStatusDto#RE_VERIFICATION_STARTED}, and
 * {@link IdentityVerificationProgressStatusDto#VERIFICATION_INFORMATION_REQUESTED_REQUIRED} /
 * {@link IdentityVerificationProgressStatusDto#VERIFICATION_INFORMATION_REQUESTED_STARTED}.
 * For the latter two, the deadline is taken from the submission's {@code resubmitRequiredUntil}
 * (falling back to {@code initiatedAt + max-age-in-unsubmitted} if not set).
 */
@Schema(name = "IdentityVerificationProgress")
public record IdentityVerificationProgressDto(
    @Schema(description = "Current state of the identity verification process")
    IdentityVerificationProgressStatusDto status,

    @Schema(
        description = "Deadline by which the current verification step must be completed, " +
            "or null if no deadline applies for the current state.",
        example = "2024-10-29T09:35:16.809924Z",
        nullable = true
    )
    Instant maxDateForStatus
) {
    /** Convenience factory — use when no deadline applies. */
    public static IdentityVerificationProgressDto of(IdentityVerificationProgressStatusDto status) {
        return new IdentityVerificationProgressDto(status, null);
    }

    /** Convenience factory — use when a deadline is known. */
    public static IdentityVerificationProgressDto of(IdentityVerificationProgressStatusDto status, Instant maxDate) {
        return new IdentityVerificationProgressDto(status, maxDate);
    }
}
