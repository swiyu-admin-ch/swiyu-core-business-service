package ch.admin.bj.swiyu.core.business.modules.dataimport.domain;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import lombok.experimental.UtilityClass;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Demo payments. Kept apart from DemoData because DemoData has to stay in sync with TMS, which knows no payments.
 */
@UtilityClass
public class DemoPaymentData {

    /** Used when no allowed return origin is configured, i.e. while the payment feature is disabled. */
    public static final String FALLBACK_RETURN_ORIGIN = "http://localhost:8200";

    public static final List<DemoPayment> PAYMENTS = List.of(
        // default BP: a failed first attempt followed by a successful one
        new DemoPayment(
            UUID.fromString("5d0c6a1e-3f7b-4c2a-9e8d-1b2c3d4e5f01"),
            DemoData.DemoCase.BUSINESS_BP_TRUST_ONBOARDING_RE_VERIFICATION.bp.id(),
            "P-DE000001",
            DemoPayment.DemoPaymentStatus.FAILED,
            null
        ),
        new DemoPayment(
            UUID.fromString("5d0c6a1e-3f7b-4c2a-9e8d-1b2c3d4e5f02"),
            DemoData.DemoCase.BUSINESS_BP_TRUST_ONBOARDING_RE_VERIFICATION.bp.id(),
            "P-DE000002",
            DemoPayment.DemoPaymentStatus.SUCCEEDED,
            Instant.now().minus(3, ChronoUnit.DAYS)
        ),
        new DemoPayment(
            UUID.fromString("5d0c6a1e-3f7b-4c2a-9e8d-1b2c3d4e5f03"),
            DemoData.DemoCase.BP_VERIFIED_NO_PROTECTED_VERIFICATION.bp.id(),
            "P-DE000003",
            DemoPayment.DemoPaymentStatus.SUCCEEDED,
            Instant.now().minus(30, ChronoUnit.DAYS)
        ),
        new DemoPayment(
            UUID.fromString("5d0c6a1e-3f7b-4c2a-9e8d-1b2c3d4e5f04"),
            DemoData.DemoCase.BP_WANTS_TO_BE_TRUSTED.bp.id(),
            "P-DE000004",
            DemoPayment.DemoPaymentStatus.PENDING,
            null
        ),
        new DemoPayment(
            UUID.fromString("5d0c6a1e-3f7b-4c2a-9e8d-1b2c3d4e5f05"),
            DemoData.DemoCase.BUSINESS_BP_BASE_ONBOARDING_ONLY.bp.id(),
            "P-DE000005",
            DemoPayment.DemoPaymentStatus.CANCELLED,
            null
        )
    );

    public static List<UUID> ids() {
        return PAYMENTS.stream().map(DemoPayment::id).toList();
    }

    public static String returnUrl(String returnOrigin, UUID paymentId) {
        return UriComponentsBuilder.fromUriString(returnOrigin)
            .pathSegment("ui", "payment", paymentId.toString())
            .build()
            .toUriString();
    }

    /** All demo payments are for product BASE_REGISTRATION. */
    public record DemoPayment(
        @NotNull UUID id,
        @NotNull UUID businessPartnerId,
        @NotNull String referenceId,
        @NotNull DemoPaymentStatus status,
        Instant payedAt
    ) {
        public enum DemoPaymentStatus {
            PENDING,
            FAILED,
            CANCELLED,
            SUCCEEDED,
        }
    }
}
