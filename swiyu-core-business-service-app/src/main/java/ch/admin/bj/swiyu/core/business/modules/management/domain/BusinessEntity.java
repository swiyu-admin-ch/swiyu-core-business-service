package ch.admin.bj.swiyu.core.business.modules.management.domain;

import static ch.admin.bj.swiyu.core.business.common.validation.EmailValidation.EMAIL_REGEX;

import ch.admin.bj.swiyu.core.business.common.domain.Address;
import ch.admin.bj.swiyu.core.business.common.domain.AuditMetadata;
import ch.admin.bj.swiyu.core.business.common.domain.BusinessPartnerType;
import com.google.common.annotations.VisibleForTesting;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Representation of a business entity (a private person, government organization or corporation) onboarded on the core service.
 * Only the contact can be updated after creation.
 */
@Entity
@Getter
@EntityListeners(AuditingEntityListener.class)
public class BusinessEntity {

    @Embedded
    @Valid
    private final AuditMetadata auditMetadata = new AuditMetadata();

    // Setter required for test data
    @Setter
    @Id
    private UUID id;

    @Setter
    private String name;

    @NotNull
    @Pattern(regexp = EMAIL_REGEX)
    private String contactEmail;

    @Enumerated(EnumType.STRING)
    @Setter
    @NotNull
    private BusinessPartnerType type;

    @Version
    @NotNull
    private Long version;

    @NotNull
    private int payedForDidSlots;

    @NotNull
    private boolean payedForTrustVerification;

    /**
     * Current aggregated state of the trust process
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    private BusinessEntityTrustStatus trustVerificationStatus;

    /**
     * Time limit, if necessary, of the current aggregated state of the trust process
     */
    private Instant maxDateForTrustVerificationStatus;

    @Embedded
    private Address address;

    private String uid;

    private String contactPhone;

    /**
     * @deprecated since 1.13.35. This will be removed once EIDARTFE_1122_TRUST_ONBOARDING feature toggle
     * is enabled on all stages and "/api/v1/internal/management/business-partners/" is removed.
     */
    @SuppressWarnings("java:S1133")
    @Deprecated(since = "1.13.35")
    public BusinessEntity(UUID id, String name, String contactEmail, BusinessPartnerType type) {
        this.id = id;
        this.name = name;
        this.contactEmail = contactEmail;
        this.type = type;
        this.trustVerificationStatus = BusinessEntityTrustStatus.NOT_VERIFIED;
        this.payedForDidSlots = 0;
        this.payedForTrustVerification = false;
    }

    public BusinessEntity(
        UUID id,
        String name,
        String contactEmail,
        BusinessPartnerType type,
        Address address,
        String uid,
        String contactPhone
    ) {
        this.id = id;
        this.name = name;
        this.contactEmail = contactEmail;
        this.type = type;
        this.trustVerificationStatus = BusinessEntityTrustStatus.NOT_VERIFIED;
        this.payedForDidSlots = 0;
        this.payedForTrustVerification = false;
        this.address = address;
        this.uid = uid;
        this.contactPhone = contactPhone;
    }

    protected BusinessEntity() {
        // JPA
    }

    public BusinessEntity update(String name, String contactEmail, Address address, String uid, String contactPhone) {
        this.name = name;
        this.contactEmail = contactEmail;
        this.address = address;
        this.uid = uid;
        this.contactPhone = contactPhone;
        return this;
    }

    public void setTrustVerificationStatus(
        BusinessEntityTrustStatus trustVerificationStatus,
        Instant maxDateForTrustVerificationStatus
    ) {
        this.trustVerificationStatus = trustVerificationStatus;
        this.maxDateForTrustVerificationStatus = maxDateForTrustVerificationStatus;
    }

    public void addPayedForDidSlots(int slots) {
        this.payedForDidSlots += slots;
    }

    public void payedForTrustVerification() {
        this.payedForTrustVerification = true;
    }

    @VisibleForTesting
    public void overwriteFrom(BusinessEntity source) {
        // id & version cannot be overwritten (DemoData constraints)
        this.name = source.name;
        this.contactEmail = source.contactEmail;
        this.type = source.type;
        this.payedForDidSlots = source.payedForDidSlots;
        this.payedForTrustVerification = source.payedForTrustVerification;
        this.trustVerificationStatus = source.trustVerificationStatus;
    }
}
