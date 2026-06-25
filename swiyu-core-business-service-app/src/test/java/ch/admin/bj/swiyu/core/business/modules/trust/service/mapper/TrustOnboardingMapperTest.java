package ch.admin.bj.swiyu.core.business.modules.trust.service.mapper;

import static ch.admin.bj.swiyu.core.business.test.TrustOnboardingSubmissionTestData.trustOnboardingSubmission;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.bj.swiyu.core.business.common.api.AddressDto;
import ch.admin.bj.swiyu.core.business.common.api.BusinessPartnerTypeDto;
import ch.admin.bj.swiyu.core.business.common.api.ContactDto;
import ch.admin.bj.swiyu.core.business.common.api.LanguageDto;
import ch.admin.bj.swiyu.core.business.common.api.MultiLanguageTextDto;
import ch.admin.bj.swiyu.core.business.common.domain.Address;
import ch.admin.bj.swiyu.core.business.common.domain.BusinessPartnerType;
import ch.admin.bj.swiyu.core.business.common.domain.Contact;
import ch.admin.bj.swiyu.core.business.common.domain.Language;
import ch.admin.bj.swiyu.core.business.common.domain.MultiLanguageText;
import ch.admin.bj.swiyu.core.business.common.service.mapper.AddressMapper;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

class TrustOnboardingMapperTest {

    private static Stream<TrustOnboardingSubmissionStatus> supportedStatus() {
        return Stream.of(
            TrustOnboardingSubmissionStatus.UNSUBMITTED,
            TrustOnboardingSubmissionStatus.SUBMITTED,
            TrustOnboardingSubmissionStatus.SUCCEEDED,
            TrustOnboardingSubmissionStatus.REJECTED,
            TrustOnboardingSubmissionStatus.INFORMATION_REQUESTED
        );
    }

    @Test
    @SuppressWarnings("deprecation") // Testing backward compatibility during transition
    void toTrustOnboardingSubmissionDto_whenGovernmental_thenCorrectlyMapped() {
        // given
        var entityName = MultiLanguageText.builder().de("de").fr("fr").it("it").en("en").rm("rm").build();
        var address = Address.builder().street("street").postalCode("postal").city("city").country("country").build();
        var contact = Contact.builder()
            .firstName("first")
            .lastName("last")
            .email("email@email.com")
            .phone("123")
            .build();
        var submission = new TrustOnboardingSubmission(
            UUID.randomUUID(),
            entityName,
            address,
            "entity@email.com",
            contact,
            Language.DE,
            "CHE-123.456.789",
            true,
            emptyList(),
            BusinessPartnerType.GOVERNMENTAL_INSTITUTION,
            SigningRule.SINGLE_SIGNATURE,
            List.of(new Signatory("John", "Doe", "+41 79 123 45 67", "john.doe@example.com"))
        );

        // when
        var result = TrustOnboardingMapper.toTrustOnboardingSubmissionDto(submission);

        // then
        assertThat(result.id()).isEqualTo(submission.getId());
        assertThat(result.version()).isEqualTo(submission.getVersion());
        assertThat(result.partnerId()).isEqualTo(submission.getPartnerId());
        assertThat(result.entityName()).isEqualTo(TrustOnboardingMapper.toMultiLanguageTextDto(entityName));
        assertThat(result.entityEmail()).isEqualTo(submission.getEntityEmail());
        assertThat(result.address()).isEqualTo(AddressMapper.toAddressDto(address));
        assertThat(result.status().name()).isEqualTo(submission.getStatus().name());
        assertThat(result.businessPartnerType()).isEqualTo(BusinessPartnerTypeDto.GOVERNMENTAL_INSTITUTION);
        assertThat(result.proofOfPossessions()).isEmpty();
        assertThat(result.correspondingLanguage()).isEqualTo(TrustOnboardingMapper.toLanguageDto(Language.DE));
        assertThat(result.registryIds()).isEqualTo(Map.of("UID", "CHE-123.456.789"));
        assertThat(result.submittedAt()).isEqualTo(submission.getSubmittedAt());
        assertThat(result.contactPerson()).isEqualTo(TrustOnboardingMapper.toContactDto(contact));
        assertThat(result.partnerNote()).isEqualTo(submission.getPartnerNote());
    }

    @Test
    @SuppressWarnings("deprecation") // Testing backward compatibility during transition
    void toTrustOnboardingSubmissionDto_whenNotGovernmental_thenCorrectlyMapped() {
        // given
        var submission = trustOnboardingSubmission();
        submission.setRequestedPartnerType(BusinessPartnerType.BUSINESS);

        // when
        var result = TrustOnboardingMapper.toTrustOnboardingSubmissionDto(submission);

        // then
        assertThat(result.businessPartnerType()).isEqualTo(BusinessPartnerTypeDto.BUSINESS);
    }

    @Test
    @SuppressWarnings("deprecation") // Testing backward compatibility during transition
    void toTrustOnboardingSubmissionDto_whenUnknown_thenCorrectlyMapped() {
        // given
        var submission = trustOnboardingSubmission();
        submission.setRequestedPartnerType(BusinessPartnerType.UNKNOWN);

        // when
        var result = TrustOnboardingMapper.toTrustOnboardingSubmissionDto(submission);

        // then
        assertThat(result.businessPartnerType()).isEqualTo(BusinessPartnerTypeDto.UNKNOWN);
    }

    @Test
    @SuppressWarnings("deprecation") // Testing backward compatibility during transition
    void toTrustOnboardingSubmissionDto_whenIndividual_thenCorrectlyMapped() {
        // given
        var submission = trustOnboardingSubmission();
        submission.setRequestedPartnerType(BusinessPartnerType.INDIVIDUAL);

        // when
        var result = TrustOnboardingMapper.toTrustOnboardingSubmissionDto(submission);

        // then
        assertThat(result.businessPartnerType()).isEqualTo(BusinessPartnerTypeDto.INDIVIDUAL);
    }

    @Test
    void toDto_forAddress_mapsCorrectly() {
        var address = new Address("street", "city", "postal", "country", "region");
        var dto = AddressMapper.toAddressDto(address);
        assertThat(dto.street()).isEqualTo("street");
        assertThat(dto.city()).isEqualTo("city");
        assertThat(dto.postalCode()).isEqualTo("postal");
        assertThat(dto.country()).isEqualTo("country");
    }

    @Test
    void toAddressEntity_forAddressDto_mapsCorrectly() {
        var dto = new AddressDto("street", "city", "postal", "country", "region");
        var entity = AddressMapper.toAddressEntity(dto);
        assertThat(entity.getStreet()).isEqualTo("street");
        assertThat(entity.getCity()).isEqualTo("city");
        assertThat(entity.getPostalCode()).isEqualTo("postal");
        assertThat(entity.getCountry()).isEqualTo("country");
    }

    @Test
    void toDto_forLanguage_mapsCorrectly() {
        assertThat(TrustOnboardingMapper.toLanguageDto(Language.DE)).isEqualTo(LanguageDto.DE);
        assertThat(TrustOnboardingMapper.toLanguageDto(Language.FR)).isEqualTo(LanguageDto.FR);
        assertThat(TrustOnboardingMapper.toLanguageDto(Language.IT)).isEqualTo(LanguageDto.IT);
        assertThat(TrustOnboardingMapper.toLanguageDto(Language.EN)).isEqualTo(LanguageDto.EN);
        assertThat(TrustOnboardingMapper.toLanguageDto(Language.RM)).isEqualTo(LanguageDto.RM);
        assertThat(TrustOnboardingMapper.toLanguageDto((Language) null)).isNull();
    }

    @Test
    void toLanguageEntity_forLanguageDto_mapsCorrectly() {
        assertThat(TrustOnboardingMapper.toLanguageEntity(LanguageDto.DE)).isEqualTo(Language.DE);
        assertThat(TrustOnboardingMapper.toLanguageEntity(LanguageDto.FR)).isEqualTo(Language.FR);
        assertThat(TrustOnboardingMapper.toLanguageEntity(LanguageDto.IT)).isEqualTo(Language.IT);
        assertThat(TrustOnboardingMapper.toLanguageEntity(LanguageDto.EN)).isEqualTo(Language.EN);
        assertThat(TrustOnboardingMapper.toLanguageEntity(LanguageDto.RM)).isEqualTo(Language.RM);
        assertThat(TrustOnboardingMapper.toLanguageEntity((LanguageDto) null)).isNull();
    }

    @Test
    void toDto_forContact_mapsCorrectly() {
        var address = new Address("street", "city", "postal", "country", "region");
        var contact = new Contact("first", "last", "email", "phone", address);
        var dto = TrustOnboardingMapper.toContactDto(contact);
        assertThat(dto.firstName()).isEqualTo("first");
        assertThat(dto.lastName()).isEqualTo("last");
        assertThat(dto.email()).isEqualTo("email");
        assertThat(dto.phone()).isEqualTo("phone");
        assertThat(dto.address()).isNotNull();
        assertThat(dto.address().street()).isEqualTo("street");
        assertThat(dto.address().country()).isEqualTo("country");
        assertThat(dto.address().postalCode()).isEqualTo("postal");
        assertThat(dto.address().city()).isEqualTo("city");
    }

    @Test
    void toDto_forContact_withNulls_mapsCorrectly() {
        assertThat(TrustOnboardingMapper.toContactDto((Contact) null)).isNull();
        var contact = new Contact("first", "last", "email", "phone", null);
        var dto = TrustOnboardingMapper.toContactDto(contact);
        assertThat(dto.address()).isNull();
    }

    @Test
    void toContactEntity_forContactDto_mapsCorrectly() {
        var addressDto = new AddressDto("street", "city", "postal", "country", "region");
        var dto = new ContactDto("first", "last", "email", "phone", addressDto);
        var entity = TrustOnboardingMapper.toContactEntity(dto);
        assertThat(entity.getFirstName()).isEqualTo("first");
        assertThat(entity.getLastName()).isEqualTo("last");
        assertThat(entity.getEmail()).isEqualTo("email");
        assertThat(entity.getPhone()).isEqualTo("phone");
        assertThat(entity.getAddress()).isNotNull();
        assertThat(entity.getAddress().getStreet()).isEqualTo("street");
    }

    @Test
    void toContactEntity_forContactDto_withNulls_mapsCorrectly() {
        assertThat(TrustOnboardingMapper.toContactEntity((ContactDto) null)).isNull();
        var dto = new ContactDto("first", "last", "email", "phone", null);
        var entity = TrustOnboardingMapper.toContactEntity(dto);
        assertThat(entity.getAddress()).isNull();
    }

    @Test
    void toDto_forMultiLanguageText_mapsCorrectly() {
        var text = new MultiLanguageText("de", "fr", "it", "en", "rm");
        var dto = TrustOnboardingMapper.toMultiLanguageTextDto(text);
        assertThat(dto.de()).isEqualTo("de");
        assertThat(dto.fr()).isEqualTo("fr");
        assertThat(dto.it()).isEqualTo("it");
        assertThat(dto.en()).isEqualTo("en");
        assertThat(dto.rm()).isEqualTo("rm");
        assertThat(TrustOnboardingMapper.toMultiLanguageTextDto((MultiLanguageText) null)).isNull();
    }

    @Test
    void toMultiLanguageTextEntity_forMultiLanguageTextDto_mapsCorrectly() {
        var dto = new MultiLanguageTextDto("de", "fr", "it", "en", "rm");
        var entity = TrustOnboardingMapper.toMultiLanguageTextEntity(dto);
        assertThat(entity.getDe()).isEqualTo("de");
        assertThat(entity.getFr()).isEqualTo("fr");
        assertThat(entity.getIt()).isEqualTo("it");
        assertThat(entity.getEn()).isEqualTo("en");
        assertThat(entity.getRm()).isEqualTo("rm");
        assertThat(TrustOnboardingMapper.toMultiLanguageTextEntity((MultiLanguageTextDto) null)).isNull();
    }

    @Test
    void mapFromDids_mapsCorrectly() {
        var dids = List.of("did:1", "did:2");
        var result = TrustOnboardingMapper.toProofOfPossession(dids);
        assertThat(result).hasSize(2).extracting(ProofOfPossession::getDid).containsExactly("did:1", "did:2");
    }

    @Test
    void mapFromDids_whenNull_returnsEmptyList() {
        var result = TrustOnboardingMapper.toProofOfPossession(null);
        assertThat(result).isNotNull().isEmpty();
    }

    @ParameterizedTest
    @EnumSource(TrustOnboardingRejectReason.class)
    void toTrustOnboardingRejectReason_mapsCorrectly(TrustOnboardingRejectReason reason) {
        var result = TrustOnboardingMapper.toTrustOnboardingRejectReason(reason.name());
        assertThat(result).isEqualTo(reason);
    }

    @Test
    void toTrustOnboardingRejectReason_whenInvalid_maps_to_other() {
        var result = TrustOnboardingMapper.toTrustOnboardingRejectReason("INVALID_REASON");
        assertThat(result).isEqualTo(TrustOnboardingRejectReason.OTHER);
    }

    @ParameterizedTest
    @EnumSource(TrustOnboardingDeclineReason.class)
    void toTrustOnboardingDeclineReason_mapsCorrectly(TrustOnboardingDeclineReason reason) {
        var result = TrustOnboardingMapper.toTrustOnboardingDeclineReason(reason.name());
        assertThat(result).isEqualTo(reason);
    }

    @Test
    void toTrustOnboardingDeclineReason_whenInvalid_maps_to_other() {
        var result = TrustOnboardingMapper.toTrustOnboardingDeclineReason("INVALID_REASON");
        assertThat(result).isEqualTo(TrustOnboardingDeclineReason.OTHER);
    }

    @ParameterizedTest
    @MethodSource("supportedStatus")
    void toTrustOnboardingSubmissionStatusDto_mapsCorrectly(TrustOnboardingSubmissionStatus status) {
        var result = TrustOnboardingMapper.toTrustOnboardingSubmissionStatusDto(status);
        assertThat(result.name()).isEqualTo(status.name());
    }
}
