package ch.admin.bj.swiyu.core.business.test;

import static ch.admin.bj.swiyu.core.business.common.service.LocalizedMapUtil.fromLanguages;

import ch.admin.bj.swiyu.core.business.common.domain.Address;
import ch.admin.bj.swiyu.core.business.common.domain.BusinessPartnerType;
import ch.admin.bj.swiyu.core.business.modules.management.domain.BusinessEntity;
import ch.admin.bj.swiyu.core.business.modules.management.domain.BusinessPartnerIdentity;
import ch.admin.bj.swiyu.core.business.modules.management.domain.BusinessPartnerIdentityStatus;
import ch.admin.bj.swiyu.core.business.modules.management.domain.BusinessPartnerRepository;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class BusinessEntityTestData {

    public static final String UNKNOWN_ENTITY_S = "11111111-1111-1111-1111-111111111111";
    public static final UUID UNKNOWN_ENTITY = UUID.fromString(UNKNOWN_ENTITY_S);

    public static final String ENTITY_A_S = "deadbeef-0000-0000-0000-000000000000";
    public static final UUID ENTITY_A = UUID.fromString(ENTITY_A_S);

    public static final String ENTITY_B_S = "deadbeef-deaf-0000-0000-000000000000";
    public static final UUID ENTITY_B = UUID.fromString(ENTITY_B_S);

    public static final String ENTITY_C_S = "deadbeef-deaf-beef-0000-000000000000";
    public static final UUID ENTITY_C = UUID.fromString(ENTITY_C_S);

    // Alias for ENTITY_A
    public static final String DEFAULT_ENTITY_S = ENTITY_A_S;
    public static final UUID DEFAULT_ENTITY = UUID.fromString(DEFAULT_ENTITY_S);

    public static BusinessEntity businessPartnerOfTypeBusiness(UUID partnerId) {
        return new BusinessEntity(
            partnerId,
            "Hello World AG",
            "hello.world@example.com",
            BusinessPartnerType.BUSINESS,
            address(),
            "CHE-123.456.789",
            "+41 78 1234567"
        );
    }

    @SuppressWarnings({ "java:S1874" }) // Remove with EID-6656
    public static BusinessEntity businessPartnerOfTypeUnknown(UUID partnerId) {
        return new BusinessEntity(
            partnerId,
            "Unkown Name",
            "unkown@example.com",
            BusinessPartnerType.UNKNOWN,
            address(),
            null,
            "+41 78 1234567"
        );
    }

    public static BusinessEntity businessPartnerOfTypeGov(UUID partnerId) {
        return new BusinessEntity(
            partnerId,
            "Gov Name",
            "gov@example.com",
            BusinessPartnerType.GOVERNMENTAL_INSTITUTION,
            address(),
            null,
            "+41 78 1234567"
        );
    }

    /**
     * A partner whose contact person has no email address. Partner notification emails are skipped for
     * such a partner - the publisher logs an error instead of failing the surrounding transaction.
     */
    public static BusinessEntity businessPartnerWithoutContactEmail(UUID partnerId) {
        return new BusinessEntity(
            partnerId,
            "No Contact AG",
            "",
            BusinessPartnerType.BUSINESS,
            address(),
            "CHE-123.456.789",
            "+41 78 1234567"
        );
    }

    public static void insertTestBusinessPartners(BusinessPartnerRepository businessPartnerRepository) {
        businessPartnerRepository.deleteAll();
        businessPartnerRepository.save(businessPartnerA());
        businessPartnerRepository.save(businessPartnerB());
        businessPartnerRepository.save(businessPartnerC());
    }

    public static BusinessEntity businessPartnerDefault() {
        return businessPartnerA();
    }

    public static BusinessEntity businessPartnerA() {
        var entityA = new BusinessEntity(
            UUID.randomUUID(),
            "Hello World AG",
            "hello.world@example.com",
            BusinessPartnerType.GOVERNMENTAL_INSTITUTION,
            address(),
            "CHE-123.456.789",
            "+41 78 1234567"
        );
        entityA.setId(ENTITY_A);
        entityA.payedForTrustVerification();
        entityA.addPayedForDidSlots(100);
        // Note: no BPI set — BPI only arrives via TMS events (TiBusinessPartnerIdentityActivatedEvent).
        // Tests that require a partner with an ACTIVE or DEACTIVATED BPI must set it explicitly
        // using applyBusinessPartnerIdentityEvent(activeBusinessPartnerIdentity()).
        return entityA;
    }

    @SuppressWarnings({ "java:S1874" }) // Remove with EID-6656
    public static BusinessEntity businessPartnerB() {
        var entityB = new BusinessEntity(
            UUID.randomUUID(),
            "FooBar GmbH",
            "foobar@example.com",
            BusinessPartnerType.UNKNOWN,
            address(),
            "CHE-123.456.789",
            "+41 78 1234567"
        );
        entityB.setId(ENTITY_B);
        return entityB;
    }

    @SuppressWarnings({ "java:S1874" }) // Remove with EID-6656
    public static BusinessEntity businessPartnerC() {
        var entityC = new BusinessEntity(
            UUID.randomUUID(),
            "Hello Second Entry AG",
            "foobar@example.com",
            BusinessPartnerType.UNKNOWN,
            address(),
            "CHE-123.456.789",
            "+41 78 1234567"
        );
        entityC.setId(ENTITY_C);
        return entityC;
    }

    private static Address address() {
        return new Address("Musterstrasse 1", "Bern", "3000", "Switzerland", "BE");
    }

    public static Map<String, String> entityNameLocalizedMap() {
        return fromLanguages(
            "Test Entity Name DE",
            "Test Entity Name DE",
            "Test Entity Name FR",
            "Test Entity Name IT",
            "Test Entity Name EN",
            "Test Entity Name RM"
        );
    }

    public static BusinessPartnerIdentity activeBusinessPartnerIdentity() {
        return activeBusinessPartnerIdentityValidUntil(null);
    }

    /**
     * An active Trust Identity that expires at the given point in time. For the renewal reminders,
     * which select partners by exactly that date.
     */
    public static BusinessPartnerIdentity activeBusinessPartnerIdentityValidUntil(Instant validUntil) {
        return BusinessPartnerIdentity.builder()
            .status(BusinessPartnerIdentityStatus.ACTIVE)
            .uid("CHE-123.456.789")
            .entityName(Map.of("default", "Hello World AG"))
            .validUntil(validUntil)
            .build();
    }

    public static BusinessPartnerIdentity deactivatedBusinessPartnerIdentity() {
        return BusinessPartnerIdentity.builder()
            .status(BusinessPartnerIdentityStatus.DEACTIVATED)
            .uid("CHE-123.456.789")
            .entityName(Map.of("default", "Hello World AG"))
            .build();
    }
}
