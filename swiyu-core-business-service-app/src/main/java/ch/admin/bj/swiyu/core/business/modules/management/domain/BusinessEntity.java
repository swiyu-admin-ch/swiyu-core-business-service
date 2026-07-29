package ch.admin.bj.swiyu.core.business.modules.management.domain;

import static ch.admin.bj.swiyu.core.business.common.service.LocalizedMapUtil.fromSingleName;

import ch.admin.bj.swiyu.core.business.common.domain.Address;
import ch.admin.bj.swiyu.core.business.common.domain.AuditMetadata;
import ch.admin.bj.swiyu.core.business.common.domain.BusinessPartnerType;
import ch.admin.bj.swiyu.core.business.common.domain.Contact;
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

    @Embedded
    private Address address;

    private String uid;

    @Embedded
    @AttributeOverride(name = "email", column = @Column(name = "contact_email"))
    @AttributeOverride(name = "phone", column = @Column(name = "contact_phone"))
    @AttributeOverride(name = "correspondingLanguage", column = @Column(name = "contact_corresponding_language"))
    @AttributeOverride(
        name = "firstName",
        column = @Column(name = "contact_first_name", insertable = false, updatable = false)
    )
    @AttributeOverride(
        name = "lastName",
        column = @Column(name = "contact_last_name", insertable = false, updatable = false)
    )
    @Valid
    private Contact contact;

    @Embedded
    @AttributeOverride(name = "validUntil", column = @Column(name = "bpi_valid_until"))
    @AttributeOverride(
        name = "trustedIdentifier",
        column = @Column(name = "bpi_trusted_identifier", columnDefinition = "jsonb")
    )
    @AttributeOverride(name = "status", column = @Column(name = "bpi_status"))
    @AttributeOverride(name = "lastActivated", column = @Column(name = "bpi_last_activated"))
    @AttributeOverride(name = "uid", column = @Column(name = "bpi_uid"))
    @AttributeOverride(name = "entityName", column = @Column(name = "bpi_entity_name", columnDefinition = "jsonb"))
    @AttributeOverride(name = "tmsVersion", column = @Column(name = "bpi_tms_version"))
    private BusinessPartnerIdentity businessPartnerIdentity;

    /**
     * @deprecated since 1.13.35. Will be removed once the V1 internal management endpoint is removed.
     */
    @SuppressWarnings("java:S1133")
    @Deprecated(since = "1.13.35")
    public BusinessEntity(UUID id, String name, String contactEmail, BusinessPartnerType type) {
        this.id = id;
        this.entityName = fromSingleName(name);
        this.contact = Contact.builder().email(contactEmail).build();
        this.type = type;
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
        this(id, fromSingleName(name), contactEmail, type, address, uid, contactPhone);
    }

    public BusinessEntity(
        UUID id,
        Map<String, String> entityName,
        String contactEmail,
        BusinessPartnerType type,
        Address address,
        String uid,
        String contactPhone
    ) {
        this.id = id;
        this.entityName = entityName;
        this.contact = Contact.builder().email(contactEmail).phone(contactPhone).build();
        this.type = type;
        this.payedForDidSlots = 0;
        this.payedForTrustVerification = false;
        this.address = address;
        this.uid = uid;
    }

    protected BusinessEntity() {
        // JPA
    }

    /**
     * Updates all editable fields. Used when the business partner identity is not yet ACTIVE
     * (i.e. the partner has not been verified / trusted yet) so name and uid can still be changed.
     */
    public BusinessEntity updateAllEditableInfo(
        Map<String, String> entityName,
        String uid,
        Contact contact,
        Address address
    ) {
        this.entityName = entityName;
        this.uid = uid;
        this.contact = contact;
        this.address = address;
        return this;
    }

    /**
     * Updates only the irrelevant (non-verified) fields. Used when the business partner identity
     * is ACTIVE — name and uid are owned by TMS and must not be overwritten from the portal.
     */
    public BusinessEntity updateIrrelevantInfo(Contact contact, Address address) {
        this.contact = contact;
        this.address = address;
        return this;
    }

    /**
     * Applies new BusinessPartnerIdentity data from a TMS BPI event.
     * This is the only path through which businessPartnerIdentity may be set.
     */
    public void applyBusinessPartnerIdentityEvent(BusinessPartnerIdentity bpi) {
        this.businessPartnerIdentity = bpi;
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
        this.businessPartnerIdentity = source.businessPartnerIdentity;
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
}
