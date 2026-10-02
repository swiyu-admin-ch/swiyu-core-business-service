package ch.admin.bj.swiyu.core.business.modules.management.domain;

import static ch.admin.bj.swiyu.core.business.common.service.LocalizedMapUtil.fromSingleName;

import ch.admin.bj.swiyu.core.business.common.domain.Address;
import ch.admin.bj.swiyu.core.business.common.domain.AuditMetadata;
import ch.admin.bj.swiyu.core.business.common.domain.BusinessPartnerType;
import ch.admin.bj.swiyu.core.business.common.domain.Contact;
import ch.admin.bj.swiyu.core.business.common.exceptions.BusinessDataIntegrityViolationException;
import ch.admin.bj.swiyu.core.business.common.i18n.ValidLocalizedMap;
import com.google.common.annotations.VisibleForTesting;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.type.SqlTypes;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Representation of a business entity (a private person, government organization or corporation) onboarded on the core service.
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

    @ValidLocalizedMap
    @NotNull
    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, @NotBlank @Size(max = 255) String> entityName;

    // Readonly field of {@link #entityName} with the default key, used in ORDER BY query.
    @Formula("(entity_name->>'default')")
    @Getter(AccessLevel.NONE)
    private String defaultEntityName;

    @Enumerated(EnumType.STRING)
    @NotNull
    private BusinessPartnerType type;

    @Version
    @NotNull
    private Long version;

    @NotNull
    private int payedForDidSlots;

    /**
     * First hard-delete safeguard: while false, CBS refuses every hard delete of this
     * partner, regardless of the caller. Toggled explicitly by ops via
     * {@link #allowHardDelete()} / {@link #unallowHardDelete()}; governmental institutions are
     * always locked to false.
     */
    @NotNull
    private boolean hardDeleteAllowed = true;

    @NotNull
    private boolean payedForTrustVerification;

    @Embedded
    private Address address;

    private String uid;

    @Embedded
    @AttributeOverride(name = "email", column = @Column(name = "contact_email"))
    @AttributeOverride(name = "phone", column = @Column(name = "contact_phone"))
    @AttributeOverride(name = "correspondingLanguage", column = @Column(name = "contact_corresponding_language"))
    @AttributeOverride(name = "firstName", column = @Column(name = "contact_first_name"))
    @AttributeOverride(name = "lastName", column = @Column(name = "contact_last_name"))
    @Valid
    private Contact contact;

    /**
     * Read-only: the identity is written through {@code BusinessPartnerIdentityService} only, so that
     * TMS BPI events never touch (and never version-bump) this entity. Joined on {@link #id}, as the
     * identity id is the partner id; {@link NotFoundAction#IGNORE} because a partner may have no identity yet.
     */
    @OneToOne(fetch = FetchType.EAGER)
    @NotFound(action = NotFoundAction.IGNORE)
    @JoinColumn(name = "id", referencedColumnName = "business_partner_id", insertable = false, updatable = false)
    private BusinessPartnerIdentity businessPartnerIdentity;

    /**
     * Canonical constructor. Used by the V2 create path
     * ({@code BusinessPartnerService.createBusinessPartnerV2}) with a fully populated {@link Contact}
     * (first/last name, email, phone, correspondence language). All other constructors delegate here.
     */
    public BusinessEntity(
        UUID id,
        Map<String, String> entityName,
        Contact contact,
        BusinessPartnerType type,
        Address address,
        String uid
    ) {
        this.id = id;
        this.entityName = entityName;
        this.contact = contact;
        this.type = type;
        this.payedForDidSlots = 0;
        this.payedForTrustVerification = false;
        this.address = address;
        this.uid = uid;
        applyGovernmentalHardDeleteGuard();
    }

    /**
     * Convenience constructor for callers that only have a single-language name and a flat
     * email/phone contact (no contact-person name or language): demo-data import and tests.
     */
    public BusinessEntity(
        UUID id,
        String name,
        String contactEmail,
        BusinessPartnerType type,
        Address address,
        String uid,
        String contactPhone
    ) {
        this(
            id,
            fromSingleName(name),
            Contact.builder().email(contactEmail).phone(contactPhone).build(),
            type,
            address,
            uid
        );
    }

    protected BusinessEntity() {
        // JPA
    }

    /**
     * Applies a partial update coming from the portal. Only the provided (non-null) fields are
     * changed; every other field keeps its current value so that unedited tiles are preserved.
     *
     * <p>A blank {@code name} or {@code uid} is ignored (never applied), so downstream systems
     * such as PAMS are never updated with an empty name. The caller decides whether name/uid may
     * be applied at all (they are owned by TMS once the identity is ACTIVE).
     */
    public BusinessEntity applyPartialUpdateFromPortal(String name, String uid, Contact contact, Address address) {
        if (name != null && !name.isBlank()) {
            this.entityName = fromSingleName(name);
        }
        if (uid != null && !uid.isBlank()) {
            this.uid = uid;
        }
        if (contact != null) {
            this.contact = contact;
        }
        if (address != null) {
            this.address = address;
        }
        return this;
    }

    /**
     * Changes the partner type. Changing it to GOVERNMENTAL_INSTITUTION locks the hard-delete
     * safeguard.
     */
    public void changeType(BusinessPartnerType type) {
        this.type = type;
        applyGovernmentalHardDeleteGuard();
    }

    /**
     * Arms the partner for hard delete. Refused for governmental institutions - they
     * must never be hard-deletable.
     */
    public void allowHardDelete() {
        if (type == BusinessPartnerType.GOVERNMENTAL_INSTITUTION) {
            throw new BusinessDataIntegrityViolationException(
                "Hard delete cannot be allowed for a governmental institution."
            );
        }
        this.hardDeleteAllowed = true;
    }

    /** Locks the partner against hard delete. */
    public void unallowHardDelete() {
        this.hardDeleteAllowed = false;
    }

    /**
     * Returns true if the business partner identity is currently ACTIVE (trusted).
     */
    public boolean isBusinessPartnerIdentityActive() {
        return (
            businessPartnerIdentity != null &&
            businessPartnerIdentity.getStatus() == BusinessPartnerIdentityStatus.ACTIVE
        );
    }

    /**
     * Kept for callers that still use the old update path from TrustOnboardingService.
     * Will be cleaned up in the same sprint once all callers are migrated.
     */
    public BusinessEntity update(
        Map<String, String> entityName,
        String contactEmail,
        Address address,
        String uid,
        String contactPhone
    ) {
        this.entityName = entityName;
        this.contact = Contact.builder()
            .email(contactEmail)
            .phone(contactPhone)
            .correspondingLanguage(this.contact != null ? this.contact.getCorrespondingLanguage() : null)
            .build();
        this.address = address;
        this.uid = uid;
        return this;
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
        this.entityName = source.entityName;
        this.contact = source.contact;
        this.type = source.type;
        this.payedForDidSlots = source.payedForDidSlots;
        this.payedForTrustVerification = source.payedForTrustVerification;
        this.hardDeleteAllowed = source.hardDeleteAllowed;
        applyGovernmentalHardDeleteGuard();
    }

    public void setName(Map<String, String> entityName) {
        this.entityName = entityName;
    }

    // ---------------------------------------------------------------------------
    // Convenience accessors kept for backward compatibility with existing callers
    // that read contactEmail / contactPhone directly from BusinessEntity.
    // These delegate to the embedded Contact.
    // Remove with EID-6624.
    // ---------------------------------------------------------------------------

    /**
     * @deprecated
     */
    @Deprecated(since = "3.42.5")
    @SuppressWarnings({ "java:S1874", "java:S1133" }) // Remove with EID-6624
    public String getContactEmail() {
        return contact != null ? contact.getEmail() : null;
    }

    /**
     * @deprecated
     */
    @Deprecated(since = "3.42.5")
    @SuppressWarnings({ "java:S1874", "java:S1133" }) // Remove with EID-6624
    public String getContactPhone() {
        return contact != null ? contact.getPhone() : null;
    }

    private void applyGovernmentalHardDeleteGuard() {
        if (type == BusinessPartnerType.GOVERNMENTAL_INSTITUTION) {
            this.hardDeleteAllowed = false;
        }
    }
}
