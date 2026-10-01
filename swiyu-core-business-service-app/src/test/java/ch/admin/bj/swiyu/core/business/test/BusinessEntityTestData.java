package ch.admin.bj.swiyu.core.business.test;

import static ch.admin.bj.swiyu.core.business.common.service.LocalizedMapUtil.fromLanguages;

import ch.admin.bj.swiyu.core.business.common.api.AddressDto;
import ch.admin.bj.swiyu.core.business.common.api.BusinessPartnerTypeDto;
import ch.admin.bj.swiyu.core.business.common.api.ContactDto;
import ch.admin.bj.swiyu.core.business.common.api.LanguageDto;
import ch.admin.bj.swiyu.core.business.common.domain.Address;
import ch.admin.bj.swiyu.core.business.common.domain.BusinessPartnerType;
import ch.admin.bj.swiyu.core.business.modules.management.api.CreatePartnerDto;
import ch.admin.bj.swiyu.core.business.modules.management.domain.BusinessEntity;
import ch.admin.bj.swiyu.core.business.modules.management.domain.BusinessPartnerIdentity;
import ch.admin.bj.swiyu.core.business.modules.management.domain.BusinessPartnerIdentityStatus;
import ch.admin.bj.swiyu.core.business.modules.management.domain.BusinessPartnerRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.experimental.UtilityClass;

@Getter
@UtilityClass
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

    public static BusinessEntity businessPartnerB() {
        var entityB = new BusinessEntity(
            UUID.randomUUID(),
            "FooBar GmbH",
            "foobar@example.com",
            BusinessPartnerType.BUSINESS,
            address(),
            "CHE-123.456.789",
            "+41 78 1234567"
        );
        entityB.setId(ENTITY_B);
        return entityB;
    }

    public static BusinessEntity businessPartnerC() {
        var entityC = new BusinessEntity(
            UUID.randomUUID(),
            "Hello Second Entry AG",
            "foobar@example.com",
            BusinessPartnerType.BUSINESS,
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

    public static BusinessPartnerIdentity businessPartnerIdentity() {
        return businessPartnerIdentity(Instant.now().plus(Duration.ofDays(365 * 3).minus(Duration.ofDays(10))));
    }

    public static BusinessPartnerIdentity businessPartnerIdentity(List<String> trustedIdentifier) {
        return businessPartnerIdentity(
            Instant.now().plus(Duration.ofDays(365 * 3).minus(Duration.ofDays(10))),
            trustedIdentifier
        );
    }

    /**
     * An active Trust Identity that expires at the given point in time. For the renewal reminders,
     * which select partners by exactly that date.
     */
    public static BusinessPartnerIdentity businessPartnerIdentity(Instant validUntil) {
        return businessPartnerIdentity(validUntil, List.of("did:example:partner1", "did:example:partner2"));
    }

    private static BusinessPartnerIdentity businessPartnerIdentity(Instant validUntil, List<String> trustedIdentifier) {
        return new BusinessPartnerIdentity(
            validUntil,
            trustedIdentifier,
            BusinessPartnerIdentityStatus.ACTIVE,
            Instant.now(),
            "CHE-123.456.789",
            Map.of("default", "Test Partner AG"),
            1L
        );
    }

    public static BusinessPartnerIdentity deactivatedBusinessPartnerIdentity() {
        return new BusinessPartnerIdentity(
            null,
            List.of("did:example:partner1", "did:example:partner2"),
            BusinessPartnerIdentityStatus.DEACTIVATED,
            Instant.now(),
            "CHE-123.456.789",
            Map.of("default", "Test Partner AG"),
            1L
        );
    }

    public static CreatePartnerDto createPartnerDto() {
        return new CreatePartnerDto(
            "Hallo Welt AG",
            BusinessPartnerTypeDto.BUSINESS,
            "CHE-123-456-789",
            someAddressDto(),
            someContactDto()
        );
    }

    public static AddressDto someAddressDto(String prefix) {
        return new AddressDto(prefix + "Street", prefix + "City", "3000", prefix + "Country", prefix + "Region");
    }

    public static AddressDto someAddressDto() {
        return someAddressDto("address");
    }

    public static ContactDto someContactDto() {
        return new ContactDto(
            "John",
            "Doe",
            "hello.world@example.com",
            "+41 78 123 45 67",
            LanguageDto.EN,
            someAddressDto("contact")
        );
    }
}
