package ch.admin.bj.swiyu.core.business.modules.management.domain;

import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.bj.swiyu.core.business.common.domain.Address;
import ch.admin.bj.swiyu.core.business.common.domain.BusinessPartnerType;
import ch.admin.bj.swiyu.core.business.common.domain.Contact;
import ch.admin.bj.swiyu.core.business.common.service.LocalizedMapUtil;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link BusinessEntity#applyPartialUpdateFromPortal}.
 *
 * <p>Verifies that only the provided (non-null) fields are changed and that blank
 * name/uid values are ignored so that downstream systems (e.g. PAMS) are never
 * updated with an empty name.
 */
@SuppressWarnings("java:S1874")
class BusinessEntityPartialUpdateTest {

    private BusinessEntity entity(String name) {
        return new BusinessEntity(
            UUID.randomUUID(),
            name,
            "old@example.com",
            BusinessPartnerType.BUSINESS,
            new Address("Old Street", "Old City", "8000", "CH", "Region"),
            "CHE-111.111.111",
            "+41 79 111 11 11"
        );
    }

    @Test
    void onlyContact_keepsNameUidAndAddress() {
        var entity = entity("Old Name");
        var newContact = Contact.builder()
            .firstName("A")
            .lastName("B")
            .email("a@b.ch")
            .phone("+41 79 123 45 67")
            .build();

        entity.applyPartialUpdateFromPortal(null, null, newContact, null);

        assertThat(LocalizedMapUtil.getDefaultValue(entity.getEntityName())).isEqualTo("Old Name");
        assertThat(entity.getUid()).isEqualTo("CHE-111.111.111");
        assertThat(entity.getAddress().getStreet()).isEqualTo("Old Street");
        assertThat(entity.getContact().getEmail()).isEqualTo("a@b.ch");
    }

    @Test
    void onlyAddress_keepsNameUidAndContact() {
        var entity = entity("Old Name");
        var newAddress = new Address("New Street", "New City", "1234", "CH", "New Region");

        entity.applyPartialUpdateFromPortal(null, null, null, newAddress);

        assertThat(LocalizedMapUtil.getDefaultValue(entity.getEntityName())).isEqualTo("Old Name");
        assertThat(entity.getUid()).isEqualTo("CHE-111.111.111");
        assertThat(entity.getAddress().getStreet()).isEqualTo("New Street");
        assertThat(entity.getAddress().getRegion()).isEqualTo("New Region");
        assertThat(entity.getContact().getEmail()).isEqualTo("old@example.com");
    }

    @Test
    void nameAndUid_updatesBoth() {
        var entity = entity("Old Name");

        entity.applyPartialUpdateFromPortal("New Name", "CHE-222.222.222", null, null);

        assertThat(LocalizedMapUtil.getDefaultValue(entity.getEntityName())).isEqualTo("New Name");
        assertThat(entity.getUid()).isEqualTo("CHE-222.222.222");
    }

    @Test
    void blankName_isIgnored() {
        var entity = entity("Old Name");

        entity.applyPartialUpdateFromPortal("   ", "CHE-222.222.222", null, null);

        // A blank name must never be applied so PAMS is not updated with an empty name.
        assertThat(LocalizedMapUtil.getDefaultValue(entity.getEntityName())).isEqualTo("Old Name");
        assertThat(entity.getUid()).isEqualTo("CHE-222.222.222");
    }

    @Test
    void blankUid_isIgnored() {
        var entity = entity("Old Name");

        entity.applyPartialUpdateFromPortal("New Name", "", null, null);

        assertThat(LocalizedMapUtil.getDefaultValue(entity.getEntityName())).isEqualTo("New Name");
        assertThat(entity.getUid()).isEqualTo("CHE-111.111.111");
    }
}
