package ch.admin.bj.swiyu.core.business.test;

import static ch.admin.bj.swiyu.core.business.common.service.LocalizedMapUtil.fromLanguages;
import static ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustOnboardingSubmissionStatus.*;
import static ch.admin.bj.swiyu.core.business.modules.trust.service.mapper.TrustOnboardingMapper.toContactEntity;
import static ch.admin.bj.swiyu.core.business.modules.trust.service.mapper.TrustOnboardingMapper.toLanguageEntity;
import static ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData.DEFAULT_ENTITY;
import static ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData.entityNameLocalizedMap;
import static java.util.Collections.emptyMap;
import static org.hibernate.internal.util.collections.CollectionHelper.listOf;

import ch.admin.bj.swiyu.core.business.common.api.AddressDto;
import ch.admin.bj.swiyu.core.business.common.api.ContactDto;
import ch.admin.bj.swiyu.core.business.common.api.LanguageDto;
import ch.admin.bj.swiyu.core.business.common.domain.BusinessPartnerType;
import ch.admin.bj.swiyu.core.business.common.service.mapper.AddressMapper;
import ch.admin.bj.swiyu.core.business.modules.management.api.BusinessPartnerTrustStatusDto;
import ch.admin.bj.swiyu.core.business.modules.trust.api.TrustOnboardingSubmissionRequestDto;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

public class TrustOnboardingSubmissionTestData {

    private static TrustOnboardingSubmissionRequestDto.TrustOnboardingSubmissionRequestDtoBuilder trustOnboardingSubmissionRequestDtoBuilder() {
        return TrustOnboardingSubmissionRequestDto.builder()
            .partnerId(DEFAULT_ENTITY)
            .entityName(entityNameLocalizedMap())
            .entityAddress(
                AddressDto.builder().street("Test Street").postalCode("1234").city("Test City").country("CH").build()
            )
            .correspondingLanguage(LanguageDto.DE)
            .registryIds(Map.of("UID", "CHE-123.456.789"))
            .entityEmail("test@example.com")
            .contactPerson(
                ContactDto.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .email("john.doe@example.com")
                    .phone("+41 79 123 45 67")
                    .build()
            )
            .dids(List.of("did:example:123", "did:example:abc"))
            .requestedPartnerType(ch.admin.bj.swiyu.core.business.common.api.BusinessPartnerTypeDto.BUSINESS)
            .signingRule(ch.admin.bj.swiyu.core.business.modules.trust.api.SigningRuleDto.SINGLE_SIGNATURE)
            .signatories(
                List.of(
                    new ch.admin.bj.swiyu.core.business.modules.trust.api.SignatoryDto(
                        "John",
                        "Doe",
                        "+41 79 123 45 67",
                        "john.doe@example.com"
                    )
                )
            );
    }

    public static TrustOnboardingSubmissionRequestDto trustOnboardingSubmissionRequestDto() {
        return trustOnboardingSubmissionRequestDtoBuilder().build();
    }

    public static TrustOnboardingSubmissionRequestDto trustOnboardingSubmissionRequestDtoWithoutUID() {
        return trustOnboardingSubmissionRequestDtoBuilder().registryIds(emptyMap()).build();
    }

    public static TrustOnboardingSubmissionRequestDto trustOnboardingSubmissionRequestDtoWithOnlyPartnerId(
        UUID partnerId
    ) {
        return TrustOnboardingSubmissionRequestDto.builder().partnerId(partnerId).build();
    }

    public static TrustOnboardingSubmissionRequestDto trustOnboardingSubmissionRequestDtoUpdate() {
        return trustOnboardingSubmissionRequestDtoBuilder()
            .entityName(
                fromLanguages(
                    "Updated Test Entity Name DE",
                    "Updated Test Entity Name DE",
                    "Updated Test Entity Name FR",
                    "Updated Test Entity Name IT",
                    "Updated Test Entity Name EN",
                    "Updated Test Entity Name RM"
                )
            )
            .entityEmail("updated@email.com")
            .correspondingLanguage(LanguageDto.DE)
            .entityAddress(
                AddressDto.builder()
                    .street("Updated Test Street")
                    .postalCode("5678")
                    .city("Updated Test City")
                    .country("CH")
                    .build()
            )
            .registryIds(Map.of("UID", "CHE-789.012.345"))
            .contactPerson(
                ContactDto.builder()
                    .firstName("Updated John")
                    .lastName("Updated Doe")
                    .email("john.doe@udpated.com")
                    .phone("+41 987 654 321")
                    .build()
            )
            .dids(listOf("did:example:456"))
            .build();
    }

    public static TrustOnboardingSubmission trustOnboardingSubmissionEmpty() {
        return new TrustOnboardingSubmission(DEFAULT_ENTITY, null, UNSUBMITTED);
    }

    public static TrustOnboardingSubmission trustOnboardingSubmission() {
        return trustOnboardingSubmission(UUID.randomUUID(), DEFAULT_ENTITY);
    }

    public static TrustOnboardingSubmission trustOnboardingSubmission(UUID id, UUID partnerId) {
        return trustOnboardingSubmission(id, partnerId, TrustOnboardingSubmissionStatus.UNSUBMITTED, Instant.now());
    }

    public static TrustOnboardingSubmission trustOnboardingSubmission(UUID id, UUID partnerId, Instant initiatedAt) {
        return trustOnboardingSubmission(id, partnerId, TrustOnboardingSubmissionStatus.UNSUBMITTED, initiatedAt);
    }

    public static TrustOnboardingSubmission trustOnboardingSubmission(
        UUID id,
        UUID partnerId,
        TrustOnboardingSubmissionStatus status
    ) {
        return trustOnboardingSubmission(id, partnerId, status, Instant.now());
    }

    public static TrustOnboardingSubmission trustOnboardingSubmission(
        UUID id,
        UUID partnerId,
        TrustOnboardingSubmissionStatus status,
        Instant initiatedAt
    ) {
        TrustOnboardingSubmissionRequestDto dto = trustOnboardingSubmissionRequestDto();
        var proofOfPossessions = List.of(SUBMITTED, REJECTED).contains(status)
            ? proofOfPossessionValid(dto.getDids())
            : proofOfPossessionNotSupplied(dto.getDids());
        var submission = new TrustOnboardingSubmission(
            id,
            partnerId,
            dto.getEntityName(),
            AddressMapper.toAddressEntity(dto.entityAddress()),
            dto.getEntityEmail(),
            toContactEntity(dto.getContactPerson()),
            toLanguageEntity(dto.correspondingLanguage()),
            dto.getRegistryIds().get("UID"),
            true,
            proofOfPossessions,
            BusinessPartnerType.BUSINESS,
            SigningRule.SINGLE_SIGNATURE,
            List.of(new Signatory("John", "Doe", "+41 79 123 45 67", "john.doe@example.com")),
            initiatedAt
        );
        switch (status) {
            case UNSUBMITTED_TIMEOUT -> submission.markAsExpired();
            case SUBMITTED -> submission.markAsSubmitted();
            case SUCCEEDED -> submission.markAsSucceeded();
            case REJECTED -> submission.markAsRejected(TrustOnboardingRejectReason.FRAUDULENT_ACTIVITY);
            case INFORMATION_REQUESTED -> submission.markAsInformationRequested(
                TrustOnboardingDeclineReason.MISSING_DOCUMENTS,
                "something missing"
            );
            case UNSUBMITTED -> {
                // nothing to do since default state is unsubmitted
            }
        }
        return submission;
    }

    private static List<ProofOfPossession> proofOfPossessionValid(List<String> dids) {
        return dids
            .stream()
            .map(did -> new ProofOfPossession(did, UUID.randomUUID().toString()).toValid())
            .toList();
    }

    private static List<ProofOfPossession> proofOfPossessionNotSupplied(List<String> dids) {
        return dids
            .stream()
            .map(did -> new ProofOfPossession(did, UUID.randomUUID().toString()))
            .toList();
    }

    public static Stream<Arguments> provideUpdateTrustStatus_aggregation_validation() {
        return Stream.of(
            Arguments.of(
                BusinessPartnerTrustStatusDto.NOT_VERIFIED,
                List.of(TrustOnboardingSubmissionStatus.UNSUBMITTED_TIMEOUT)
            ),
            Arguments.of(BusinessPartnerTrustStatusDto.NOT_VERIFIED, List.of(TrustOnboardingSubmissionStatus.REJECTED)),
            Arguments.of(
                BusinessPartnerTrustStatusDto.NOT_VERIFIED,
                List.of(TrustOnboardingSubmissionStatus.SUCCEEDED, TrustOnboardingSubmissionStatus.REJECTED)
            ),
            Arguments.of(
                BusinessPartnerTrustStatusDto.VERIFICATION_STARTED,
                List.of(TrustOnboardingSubmissionStatus.UNSUBMITTED)
            ),
            Arguments.of(
                BusinessPartnerTrustStatusDto.VERIFICATION_STARTED,
                List.of(TrustOnboardingSubmissionStatus.REJECTED, TrustOnboardingSubmissionStatus.UNSUBMITTED)
            ),
            Arguments.of(
                BusinessPartnerTrustStatusDto.VERIFICATION_IN_PROGRESS,
                List.of(TrustOnboardingSubmissionStatus.SUBMITTED)
            ),
            Arguments.of(
                BusinessPartnerTrustStatusDto.VERIFICATION_IN_PROGRESS,
                List.of(TrustOnboardingSubmissionStatus.REJECTED, TrustOnboardingSubmissionStatus.SUBMITTED)
            ),
            Arguments.of(BusinessPartnerTrustStatusDto.VERIFIED, List.of(TrustOnboardingSubmissionStatus.SUCCEEDED)),
            Arguments.of(
                BusinessPartnerTrustStatusDto.VERIFIED,
                List.of(TrustOnboardingSubmissionStatus.REJECTED, TrustOnboardingSubmissionStatus.SUCCEEDED)
            ),
            Arguments.of(
                BusinessPartnerTrustStatusDto.VERIFIED,
                List.of(TrustOnboardingSubmissionStatus.UNSUBMITTED, TrustOnboardingSubmissionStatus.SUCCEEDED)
            ),
            Arguments.of(
                BusinessPartnerTrustStatusDto.VERIFIED,
                List.of(TrustOnboardingSubmissionStatus.UNSUBMITTED_TIMEOUT, TrustOnboardingSubmissionStatus.SUCCEEDED)
            ),
            Arguments.of(
                BusinessPartnerTrustStatusDto.VERIFIED,
                List.of(TrustOnboardingSubmissionStatus.SUCCEEDED, TrustOnboardingSubmissionStatus.UNSUBMITTED_TIMEOUT)
            ),
            Arguments.of(
                BusinessPartnerTrustStatusDto.RE_VERIFICATION_STARTED,
                List.of(TrustOnboardingSubmissionStatus.SUCCEEDED, TrustOnboardingSubmissionStatus.UNSUBMITTED)
            ),
            Arguments.of(
                BusinessPartnerTrustStatusDto.RE_VERIFICATION_STARTED,
                List.of(TrustOnboardingSubmissionStatus.SUCCEEDED, TrustOnboardingSubmissionStatus.UNSUBMITTED)
            ),
            Arguments.of(
                BusinessPartnerTrustStatusDto.RE_VERIFICATION_IN_PROGRESS,
                List.of(TrustOnboardingSubmissionStatus.SUCCEEDED, TrustOnboardingSubmissionStatus.SUBMITTED)
            ),
            Arguments.of(
                BusinessPartnerTrustStatusDto.RE_VERIFICATION_IN_PROGRESS,
                List.of(TrustOnboardingSubmissionStatus.SUCCEEDED, SUBMITTED)
            ),
            Arguments.of(
                BusinessPartnerTrustStatusDto.INFORMATION_REQUESTED,
                List.of(
                    TrustOnboardingSubmissionStatus.UNSUBMITTED_TIMEOUT,
                    TrustOnboardingSubmissionStatus.INFORMATION_REQUESTED
                )
            ),
            Arguments.of(
                BusinessPartnerTrustStatusDto.INFORMATION_REQUESTED,
                List.of(
                    TrustOnboardingSubmissionStatus.SUCCEEDED,
                    TrustOnboardingSubmissionStatus.INFORMATION_REQUESTED
                )
            ),
            Arguments.of(
                BusinessPartnerTrustStatusDto.INFORMATION_REQUESTED,
                List.of(TrustOnboardingSubmissionStatus.REJECTED, TrustOnboardingSubmissionStatus.INFORMATION_REQUESTED)
            )
        );
    }
}
