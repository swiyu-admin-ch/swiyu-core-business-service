package ch.admin.bj.swiyu.core.business.modules.trust.infrastructure.web;

import static ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding.ProofOfPossessionKeyUtils.*;
import static ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData.multiLanguageTextDtoEntityName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import ch.admin.bj.swiyu.core.business.common.api.AddressDto;
import ch.admin.bj.swiyu.core.business.common.api.ContactDto;
import ch.admin.bj.swiyu.core.business.common.api.LanguageDto;
import ch.admin.bj.swiyu.core.business.common.did.DidPublicKeyLoader;
import ch.admin.bj.swiyu.core.business.modules.trust.api.*;
import ch.admin.bj.swiyu.core.business.modules.trust.config.TrustRegistryProperties;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustOnboardingSubmissionRepository;
import ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding.TrustOnboardingService;
import ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData;
import ch.admin.bj.swiyu.core.business.test.VCTypeMetadataTestData;
import ch.admin.bj.swiyu.core.business.test.WithExtendedJeapAuthenticationToken;
import ch.admin.bj.swiyu.core.business.test.container.WithAllTestContainerInitializers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * For JeapAuthanticationToken see the <a href="https://bitbucket.bit.admin.ch/projects/JEAP/repos/jeap-spring-boot-starters/browse/jeap-spring-boot-security-starter-test"> README examples</a>
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@WithAllTestContainerInitializers
class TrustOnboardingSubmissionB2BControllerIT {

    static final String TRUST_ONBOARDING_SUBMISSION_B2B_BASE_URL = "/api/v1/trust/trust-onboarding-submission";

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    VCTypeMetadataTestData testData;

    @Autowired
    TrustOnboardingService trustOnboardingService;

    @Autowired
    TrustOnboardingSubmissionRepository trustOnboardingSubmissionRepository;

    @MockitoBean
    private TrustRegistryProperties trustRegistryProperties;

    @MockitoBean
    DidPublicKeyLoader didPublicKeyLoader;

    @BeforeEach
    void setUp() throws MalformedURLException {
        when(trustRegistryProperties.dataServiceBaseUrl()).thenReturn(URI.create("https://test-url.ch").toURL());
        trustOnboardingSubmissionRepository.deleteAll();
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "/delete_vc_schema_submissions.sql")
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "/delete_business_entities.sql")
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "/insert_test_business_entities.sql")
    @WithExtendedJeapAuthenticationToken(
        subject = "deadbeef-0000-0000-0000-000000000000",
        bpRoles = "deadbeef-0000-0000-0000-000000000000 = ti_@trustonboardingsubmission_#read, ti_@trustonboardingsubmission_#write"
    )
    void testGetProofOfPossessions_thenSuccess() throws Exception {
        // GIVEN trust onboarding submission with dids
        var requestDto = TrustOnboardingSubmissionRequestDto.builder()
            .partnerId(BusinessEntityTestData.DEFAULT_ENTITY)
            .entityName(multiLanguageTextDtoEntityName())
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
            .build();
        trustOnboardingService.createTrustOnboardingSubmission(requestDto);

        // WHEN THEN calling the endpoint
        var result = mockMvc
            .perform(MockMvcRequestBuilders.get(TRUST_ONBOARDING_SUBMISSION_B2B_BASE_URL + "/proof-of-possessions"))
            .andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.OK.value());
        var popList = objectMapper.readValue(
            result.getResponse().getContentAsString(),
            new TypeReference<List<ProofOfPossessionDto>>() {}
        );
        assertThat(popList).hasSize(2);
        assertThat(popList).extracting("did").containsExactlyInAnyOrder("did:example:123", "did:example:abc");
        assertThat(popList).allMatch(
            pop -> pop.status() == ProofOfPossessionStatusDto.NOT_SUPPLIED && pop.verifiedAt() == null
        );
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "/delete_vc_schema_submissions.sql")
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "/delete_business_entities.sql")
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "/insert_test_business_entities.sql")
    @WithExtendedJeapAuthenticationToken(
        subject = "deadbeef-0000-0000-0000-000000000000",
        bpRoles = "deadbeef-0000-0000-0000-000000000000 = ti_@trustonboardingsubmission_#read, ti_@trustonboardingsubmission_#write"
    )
    void testGetProofOfPossessionsForNonExistingPartner_thenFails() throws Exception {
        // GIVEN trust onboarding submission with dids
        var requestDto = TrustOnboardingSubmissionRequestDto.builder()
            .partnerId(UUID.randomUUID())
            .entityName(multiLanguageTextDtoEntityName())
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
            .build();
        trustOnboardingService.createTrustOnboardingSubmission(requestDto);

        // WHEN THEN calling the endpoint
        var result = mockMvc
            .perform(MockMvcRequestBuilders.get(TRUST_ONBOARDING_SUBMISSION_B2B_BASE_URL + "/proof-of-possessions"))
            .andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(result.getResponse().getContentAsString()).contains(
            "No UNSUBMITTED TrustOnboardingSubmission found for partner"
        );
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "/delete_vc_schema_submissions.sql")
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "/delete_business_entities.sql")
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "/insert_test_business_entities.sql")
    @WithExtendedJeapAuthenticationToken(
        subject = "deadbeef-0000-0000-0000-000000000000",
        bpRoles = "deadbeef-0000-0000-0000-000000000000 = ti_@trustonboardingsubmission_#read, ti_@trustonboardingsubmission_#write"
    )
    void testSubmitProofOfPossessions_thenSuccess() throws Exception {
        // GIVEN trust onboarding submission with dids
        var requestDto = TrustOnboardingSubmissionRequestDto.builder()
            .partnerId(BusinessEntityTestData.DEFAULT_ENTITY)
            .entityName(multiLanguageTextDtoEntityName())
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
            .dids(List.of("did:example:123"))
            .build();
        var pop = trustOnboardingService.createTrustOnboardingSubmission(requestDto).proofOfPossessions().getFirst();

        // WHEN submitting proof of possessions
        var did = "did:example:123";
        var didWithFragment = did + "#key1";
        var kp1 = generateKeyPair();
        var signer1 = getSigner(kp1.getPrivate());
        var verifier1 = getVerifier(kp1.getPublic());
        var popStringList = Stream.of(getPoPSubmission(pop.nonce(), did, didWithFragment, signer1)).toList();
        when(didPublicKeyLoader.loadPublicKey(didWithFragment)).thenReturn(verifier1);
        var result = mockMvc
            .perform(
                MockMvcRequestBuilders.post(TRUST_ONBOARDING_SUBMISSION_B2B_BASE_URL + "/proof-of-possessions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new ProofOfPossessionSubmissionDto(popStringList)))
            )
            .andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.OK.value());
    }
}
