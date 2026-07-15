package ch.admin.bj.swiyu.core.business.modules.trust.infrastructure.web;

import static ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustOnboardingSubmissionStatus.SUBMITTED;
import static ch.admin.bj.swiyu.core.business.test.TrustOnboardingSubmissionTestData.trustOnboardingSubmission;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import ch.admin.bj.swiyu.antivirus.client.api.ScanApi;
import ch.admin.bj.swiyu.antivirus.client.model.ScanResult;
import ch.admin.bj.swiyu.core.business.common.audit.AuditPublisher;
import ch.admin.bj.swiyu.core.business.common.domain.Address;
import ch.admin.bj.swiyu.core.business.common.domain.Contact;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustOnboardingSubmission;
import ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData;
import ch.admin.bj.swiyu.core.business.test.TestRepositories;
import ch.admin.bj.swiyu.core.business.test.TrustOnboardingSubmissionDocumentTestData;
import ch.admin.bj.swiyu.core.business.test.WithExtendedJeapAuthenticationToken;
import ch.admin.bj.swiyu.core.business.test.container.WithAllTestContainerInitializers;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@WithAllTestContainerInitializers
@MockitoBean(types = AuditPublisher.class)
class TrustOnboardingSubmissionDocumentsInternalControllerIT {

    UUID trustOnboardingSubmissionId;
    UUID businessPartnerId;

    @MockitoBean
    ScanApi scanApi;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestRepositories testRepositories;

    @BeforeEach
    void setUp() {
        testRepositories.truncateTables();
        BusinessEntityTestData.insertTestBusinessPartners(testRepositories.businessPartner);

        // Arrange
        TrustOnboardingSubmission submission = testRepositories.trustOnboardingSubmission.save(
            trustOnboardingSubmissionWithContactAddress()
        );
        trustOnboardingSubmissionId = submission.getId();
        businessPartnerId = submission.getPartnerId();

        when(scanApi.scanGet(any())).thenReturn(
            List.of(
                new ScanResult()
                    .result("OK")
                    .requestID(UUID.randomUUID())
                    .description("description")
                    .clamavVersion("clamav-v1")
                    .clamavDatabaseVersion("clamav-db-v1")
            )
        );
    }

    @Test
    @WithExtendedJeapAuthenticationToken(
        subject = "deadbeef-0000-0000-0000-000000000000",
        bpRoles = BusinessEntityTestData.DEFAULT_ENTITY_S + " = ti_@trustonboardingsubmission_#write"
    )
    void test_DocumentUploadOk() throws Exception {
        var testDocument = TrustOnboardingSubmissionDocumentTestData.TestDocument.builder()
            .trustOnboardingSubmissionId(trustOnboardingSubmissionId)
            .build();
        var resultUpload = uploadTestDocument(testDocument);
        var documentUploadResponseJson = JsonPath.parse(resultUpload.getResponse().getContentAsString());

        assertThat(resultUpload.getResponse().getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat((String) documentUploadResponseJson.read("$.id")).isNotEmpty();
        assertThat((String) documentUploadResponseJson.read("$.type")).isEqualTo(testDocument.partnerDocumentType());
    }

    @Test
    @WithExtendedJeapAuthenticationToken(
        subject = "deadbeef-0000-0000-0000-000000000000",
        bpRoles = BusinessEntityTestData.DEFAULT_ENTITY_S + " = ti_@trustonboardingsubmission_#write"
    )
    void test_DocumentUploadDeclarationOfIntent_TriggersDoiValidation() throws Exception {
        var testDocument = TrustOnboardingSubmissionDocumentTestData.TestDocument.builder()
            .trustOnboardingSubmissionId(trustOnboardingSubmissionId)
            .partnerDocumentType("TRUST_ONBOARDING_DECLARATION_OF_INTENT")
            .build();

        var resultUpload = uploadTestDocument(testDocument);

        assertThat(resultUpload.getResponse().getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    @WithExtendedJeapAuthenticationToken(
        subject = "deadbeef-0000-0000-0000-000000000000",
        bpRoles = BusinessEntityTestData.DEFAULT_ENTITY_S + " = ti_@trustonboardingsubmission_#write"
    )
    void test_DocumentUploadVirus() throws Exception {
        when(scanApi.scanGet(any())).thenReturn(
            List.of(
                new ScanResult()
                    .result("FOUND")
                    .requestID(UUID.randomUUID())
                    .description("description")
                    .clamavVersion("clamav-v1")
                    .clamavDatabaseVersion("clamav-db-v1")
            )
        );
        var testDocument = TrustOnboardingSubmissionDocumentTestData.TestDocument.builder()
            .trustOnboardingSubmissionId(trustOnboardingSubmissionId)
            .build();

        var resultUpload = uploadTestDocument(testDocument);
        var documentUploadResponseJson = JsonPath.parse(resultUpload.getResponse().getContentAsString());

        assertThat(resultUpload.getResponse().getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat((String) documentUploadResponseJson.read("$.errorCode")).isEqualTo("data_invalid_virus_detected");
    }

    @Test
    @WithExtendedJeapAuthenticationToken(
        subject = "deadbeef-0000-0000-0000-000000000000",
        bpRoles = {
            BusinessEntityTestData.DEFAULT_ENTITY_S +
                " = ti_@trustonboardingsubmission_#read, ti_@trustonboardingsubmission_#write",
        }
    )
    void test_GetDocumentOk() throws Exception {
        var testDocument = TrustOnboardingSubmissionDocumentTestData.TestDocument.builder()
            .trustOnboardingSubmissionId(trustOnboardingSubmissionId)
            .build();
        var documentUploadResponse = uploadTestDocument(testDocument);
        var documentUploadResponseJson = JsonPath.parse(documentUploadResponse.getResponse().getContentAsString());
        String documentId = documentUploadResponseJson.read("$.id");
        var getResult = mockMvc
            .perform(
                MockMvcRequestBuilders.get(
                    "/api/v1/internal/trust/trust-onboarding-submission/{trustOnboardingSubmissionId}/document/{documentId}",
                    trustOnboardingSubmissionId,
                    documentId
                )
            )
            // Assert
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(jsonPath("$.id").value(documentId))
            .andExpect(jsonPath("$.type").value(testDocument.partnerDocumentType()))
            .andExpect(
                jsonPath("$.trustOnboardingSubmissionId").value(testDocument.trustOnboardingSubmissionId().toString())
            )
            .andExpect(jsonPath("$.owningBusinessPartner").value(BusinessEntityTestData.DEFAULT_ENTITY_S))
            .andExpect(jsonPath("$.mediaType").value(testDocument.contentType()))
            .andExpect(jsonPath("$.name").value(testDocument.fileName()))
            .andReturn();

        var getResponseJson = JsonPath.parse(getResult.getResponse().getContentAsString());
        assertThat(Instant.parse(getResponseJson.read("$.submittedAt"))).isBetween(
            Instant.now().minusSeconds(1),
            Instant.now().plusSeconds(1)
        );
    }

    @Test
    @WithExtendedJeapAuthenticationToken(
        subject = "deadbeef-0000-0000-0000-000000000000",
        bpRoles = {
            BusinessEntityTestData.DEFAULT_ENTITY_S +
                " = ti_@trustonboardingsubmission_#read, ti_@trustonboardingsubmission_#write",
        }
    )
    void test_ListDocumentOk() throws Exception {
        // GIVEN
        var testDocumentTemplate =
            TrustOnboardingSubmissionDocumentTestData.TestDocument.builder().trustOnboardingSubmissionId(
                trustOnboardingSubmissionId
            );
        var testDocument = testDocumentTemplate.build();
        uploadTestDocument(testDocument);
        uploadTestDocument(testDocument);
        uploadTestDocument(testDocument);
        var documentUploadResponse = uploadTestDocument(
            testDocumentTemplate.partnerDocumentType("TRUST_ONBOARDING_DECLARATION_OF_INTENT").build()
        );
        var documentUploadResponseJson = JsonPath.parse(documentUploadResponse.getResponse().getContentAsString());
        String documentId = documentUploadResponseJson.read("$.id");
        // WHEN / THEN
        mockMvc
            .perform(
                MockMvcRequestBuilders.get(
                    "/api/v1/internal/trust/trust-onboarding-submission/{trustOnboardingSubmissionId}/document",
                    trustOnboardingSubmissionId
                )
            )
            // Assert
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(jsonPath("$.page.number").value(0))
            .andExpect(jsonPath("$.page.totalElements").value(4))
            .andExpect(jsonPath("$.page.totalPages").value(1))
            .andExpect(
                jsonPath(
                    "$.content.[?(@.id != '" + documentId + "')].type",
                    everyItem(is(testDocument.partnerDocumentType()))
                )
            )
            .andExpect(jsonPath("$.content.[?(@.id == '" + documentId + "')].id").value(documentId))
            .andExpect(
                jsonPath("$.content.[?(@.id == '" + documentId + "')].type").value(
                    "TRUST_ONBOARDING_DECLARATION_OF_INTENT"
                )
            )
            .andExpect(
                jsonPath("$.content.[?(@.id == '" + documentId + "')].trustOnboardingSubmissionId").value(
                    testDocument.trustOnboardingSubmissionId().toString()
                )
            )
            .andExpect(
                jsonPath("$.content.[?(@.id == '" + documentId + "')].owningBusinessPartner").value(
                    BusinessEntityTestData.DEFAULT_ENTITY_S
                )
            )
            .andExpect(
                jsonPath("$.content.[?(@.id == '" + documentId + "')].mediaType").value(testDocument.contentType())
            )
            .andExpect(jsonPath("$.content.[?(@.id == '" + documentId + "')].name").value(testDocument.fileName()))
            .andReturn();
    }

    @Test
    @WithExtendedJeapAuthenticationToken(
        subject = BusinessEntityTestData.ENTITY_A_S,
        bpRoles = {
            BusinessEntityTestData.DEFAULT_ENTITY_S +
                " = ti_@trustonboardingsubmission_#read, ti_@trustonboardingsubmission_#write",
        }
    )
    void test_DocumentDeleteOk() throws Exception {
        var testDocument = TrustOnboardingSubmissionDocumentTestData.TestDocument.builder()
            .trustOnboardingSubmissionId(trustOnboardingSubmissionId)
            .build();
        var uploadResult = uploadTestDocument(testDocument);
        String documentId = JsonPath.parse(uploadResult.getResponse().getContentAsString()).read("$.id");

        mockMvc
            .perform(
                MockMvcRequestBuilders.delete(
                    "/api/v1/internal/trust/trust-onboarding-submission/{trustOnboardingSubmissionId}/document/{documentId}",
                    trustOnboardingSubmissionId,
                    documentId
                )
            )
            .andExpect(MockMvcResultMatchers.status().isNoContent());

        assertThat(testRepositories.partnerDocuments.count()).isZero();
    }

    @Test
    @WithExtendedJeapAuthenticationToken(
        subject = BusinessEntityTestData.ENTITY_C_S,
        bpRoles = BusinessEntityTestData.ENTITY_C_S + " = ti_@trustonboardingsubmission_#write"
    )
    void test_DocumentDeleteOnSubmittedSubmission_Returns400() throws Exception {
        var submittedSubmission = testRepositories.trustOnboardingSubmission.save(
            trustOnboardingSubmission(UUID.randomUUID(), BusinessEntityTestData.ENTITY_C, SUBMITTED)
        );

        mockMvc
            .perform(
                MockMvcRequestBuilders.delete(
                    "/api/v1/internal/trust/trust-onboarding-submission/{trustOnboardingSubmissionId}/document/{documentId}",
                    submittedSubmission.getId(),
                    UUID.randomUUID()
                )
            )
            .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    @WithExtendedJeapAuthenticationToken(
        subject = BusinessEntityTestData.ENTITY_A_S,
        bpRoles = {
            BusinessEntityTestData.DEFAULT_ENTITY_S + " = ti_@trustonboardingsubmission_#write",
            BusinessEntityTestData.ENTITY_C_S + " = ti_@trustonboardingsubmission_#write",
        }
    )
    void test_DocumentDeleteCrossSubmission_Returns403() throws Exception {
        var otherSubmission = testRepositories.trustOnboardingSubmission.save(
            trustOnboardingSubmission(UUID.randomUUID(), BusinessEntityTestData.ENTITY_C)
        );
        var docInOtherSubmission = TrustOnboardingSubmissionDocumentTestData.TestDocument.builder()
            .trustOnboardingSubmissionId(otherSubmission.getId())
            .build();
        var uploadResult = uploadTestDocument(docInOtherSubmission);
        String documentId = JsonPath.parse(uploadResult.getResponse().getContentAsString()).read("$.id");

        mockMvc
            .perform(
                MockMvcRequestBuilders.delete(
                    "/api/v1/internal/trust/trust-onboarding-submission/{trustOnboardingSubmissionId}/document/{documentId}",
                    trustOnboardingSubmissionId,
                    documentId
                )
            )
            .andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    @WithExtendedJeapAuthenticationToken(
        subject = BusinessEntityTestData.ENTITY_A_S,
        bpRoles = BusinessEntityTestData.DEFAULT_ENTITY_S + " = ti_@trustonboardingsubmission_#write"
    )
    void test_DocumentDeleteNotFound_Returns400() throws Exception {
        mockMvc
            .perform(
                MockMvcRequestBuilders.delete(
                    "/api/v1/internal/trust/trust-onboarding-submission/{trustOnboardingSubmissionId}/document/{documentId}",
                    trustOnboardingSubmissionId,
                    UUID.randomUUID()
                )
            )
            .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    @WithExtendedJeapAuthenticationToken(
        subject = "deadbeef-0000-0000-0000-000000000000",
        bpRoles = BusinessEntityTestData.ENTITY_B_S + " = ti_@trustonboardingsubmission_#write"
    )
    void test_CrossBusinessPartnerUploadPermissionForbidden() throws Exception {
        var resultUpload = uploadTestDocument(
            TrustOnboardingSubmissionDocumentTestData.TestDocument.builder()
                .trustOnboardingSubmissionId(trustOnboardingSubmissionId)
                .build()
        );
        assertThat(resultUpload.getResponse().getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
    }

    @Test
    @WithExtendedJeapAuthenticationToken(
        subject = "deadbeef-0000-0000-0000-000000000000",
        bpRoles = {
            BusinessEntityTestData.DEFAULT_ENTITY_S +
                " = ti_@trustonboardingsubmission_#read, ti_@trustonboardingsubmission_#write",
        }
    )
    void test_GetDeclarationOfIntentDocumentForTrustOnboarding_AsAttachment() throws Exception {
        var expectedFilename = "declaration-of-intent-%s-%s-de.pdf".formatted(
            businessPartnerId,
            trustOnboardingSubmissionId
        );

        var result = mockMvc
            .perform(
                MockMvcRequestBuilders.get(
                    "/api/v1/internal/trust/trust-onboarding-submission/{trustOnboardingSubmissionId}/document/doi",
                    trustOnboardingSubmissionId
                ).param("language", "DE")
            )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(MockMvcResultMatchers.header().string("Content-Disposition", containsString("attachment;")))
            .andExpect(MockMvcResultMatchers.header().string("Content-Disposition", containsString(expectedFilename)))
            .andReturn();

        assertThat(result.getResponse().getContentAsByteArray()).isNotEmpty();
        assertThat(new String(result.getResponse().getContentAsByteArray(), 0, 4)).isEqualTo("%PDF");
    }

    private MvcResult uploadTestDocument(TrustOnboardingSubmissionDocumentTestData.TestDocument testDocument)
        throws Exception {
        MockMultipartFile multipartFile = TrustOnboardingSubmissionDocumentTestData.getTestFile(testDocument);

        return mockMvc
            .perform(
                MockMvcRequestBuilders.multipart(
                    "/api/v1/internal/trust/trust-onboarding-submission/{trustOnboardingSubmissionId}/document",
                    testDocument.trustOnboardingSubmissionId()
                )
                    .file(multipartFile)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .accept(MediaType.APPLICATION_JSON)
                    .param("type", testDocument.partnerDocumentType())
            )
            .andReturn();
    }

    private static TrustOnboardingSubmission trustOnboardingSubmissionWithContactAddress() {
        var base = trustOnboardingSubmission();
        var contactWithAddress = Contact.builder()
            .firstName(base.getContactPerson().getFirstName())
            .lastName(base.getContactPerson().getLastName())
            .email(base.getContactPerson().getEmail())
            .phone(base.getContactPerson().getPhone())
            .address(Address.builder().street("Test Street").city("Test City").postalCode("1234").country("CH").build())
            .build();

        return new TrustOnboardingSubmission(
            base.getId(),
            base.getPartnerId(),
            base.getEntityName(),
            base.getEntityAddress(),
            base.getEntityEmail(),
            contactWithAddress,
            base.getCorrespondingLanguage(),
            base.getUid(),
            base.getIsRegisteredInCommercialRegister(),
            base.getProofOfPossessions(),
            base.getRequestedPartnerType(),
            base.getSigningRule(),
            base.getSignatories(),
            Instant.now()
        );
    }
}
