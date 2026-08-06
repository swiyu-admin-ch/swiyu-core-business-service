package ch.admin.bj.swiyu.core.business.modules.trust.infrastructure.web;

import static ch.admin.bj.swiyu.core.business.test.ProtectedVerificationSubmissionTestData.protectedVerificationSubmission;
import static ch.admin.bj.swiyu.core.business.test.ProtectedVerificationSubmissionTestData.protectedVerificationSubmissionRequestDto;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.admin.bj.swiyu.core.business.modules.trust.api.ProtectedVerificationCategoryDto;
import ch.admin.bj.swiyu.core.business.modules.trust.api.ProtectedVerificationSubmissionDto;
import ch.admin.bj.swiyu.core.business.modules.trust.api.ProtectedVerificationSubmissionListItemDto;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.publisher.DomainEventPublisher;
import ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData;
import ch.admin.bj.swiyu.core.business.test.RestResponsePage;
import ch.admin.bj.swiyu.core.business.test.TestRepositories;
import ch.admin.bj.swiyu.core.business.test.WithExtendedJeapAuthenticationToken;
import ch.admin.bj.swiyu.core.business.test.container.WithAllTestContainerInitializers;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@WithAllTestContainerInitializers
class ProtectedVerificationSubmissionInternalControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestRepositories testRepositories;

    @MockitoBean
    private DomainEventPublisher domainEventPublisher;

    @BeforeEach
    void setUp() {
        testRepositories.truncateTables();
        BusinessEntityTestData.insertTestBusinessPartners(testRepositories.businessPartner);
        var trustedEntity = testRepositories.businessPartner
            .findById(BusinessEntityTestData.DEFAULT_ENTITY)
            .orElseThrow();
        trustedEntity.applyBusinessPartnerIdentityEvent(BusinessEntityTestData.activeBusinessPartnerIdentity());
        testRepositories.businessPartner.save(trustedEntity);
    }

    @Test
    @WithExtendedJeapAuthenticationToken(
        subject = "deadbeef-0000-0000-0000-000000000000",
        bpRoles = "deadbeef-0000-0000-0000-000000000000 = ti_@protectedverificationsubmission_#read"
    )
    void testGetProtectedVerificationSubmissions() throws Exception {
        // Arrange
        testRepositories.protectedVerificationSubmission.save(
            protectedVerificationSubmission(BusinessEntityTestData.DEFAULT_ENTITY)
        );
        testRepositories.protectedVerificationSubmission.save(
            protectedVerificationSubmission(BusinessEntityTestData.ENTITY_B)
        );

        // Act
        var submissions = objectMapper.readValue(
            mockMvc
                .perform(MockMvcRequestBuilders.get("/api/v1/internal/trust/protected-verification-submissions"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            new TypeReference<RestResponsePage<ProtectedVerificationSubmissionListItemDto>>() {}
        );

        // Assert
        assertThat(submissions.getContent()).hasSize(1);
        assertThat(submissions.getContent().getFirst().partnerId()).isEqualTo(BusinessEntityTestData.DEFAULT_ENTITY);
    }

    @Test
    @WithExtendedJeapAuthenticationToken(
        subject = "deadbeef-0000-0000-0000-000000000000",
        bpRoles = "deadbeef-0000-0000-0000-000000000000 = ti_@protectedverificationsubmission_#read"
    )
    void testGetProtectedVerificationCategories() throws Exception {
        // Act
        var categories = objectMapper.readValue(
            mockMvc
                .perform(
                    MockMvcRequestBuilders.get("/api/v1/internal/trust/protected-verification-submissions/categories")
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            new TypeReference<List<ProtectedVerificationCategoryDto>>() {}
        );

        // Assert
        assertThat(categories).containsExactly(ProtectedVerificationCategoryDto.PERSONAL_ADMINISTRATIVE_NUMBER);
    }

    @Test
    @WithExtendedJeapAuthenticationToken(
        subject = "deadbeef-0000-0000-0000-000000000000",
        bpRoles = "deadbeef-0000-0000-0000-000000000000 = ti_@protectedverificationsubmission_#read"
    )
    void testGetProtectedVerificationSubmission() throws Exception {
        // Arrange
        var submission = testRepositories.protectedVerificationSubmission.save(
            protectedVerificationSubmission(BusinessEntityTestData.DEFAULT_ENTITY)
        );

        // Act
        var result = mockMvc
            .perform(
                MockMvcRequestBuilders.get(
                    "/api/v1/internal/trust/protected-verification-submissions/{id}",
                    submission.getId()
                )
            )
            .andReturn();

        // Assert
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    @WithExtendedJeapAuthenticationToken(
        subject = "deadbeef-0000-0000-0000-000000000000",
        bpRoles = "deadbeef-0000-0000-0000-000000000000 = ti_@protectedverificationsubmission_#write"
    )
    void testCreateProtectedVerificationSubmission() throws Exception {
        // Arrange
        var requestDto = protectedVerificationSubmissionRequestDto(BusinessEntityTestData.DEFAULT_ENTITY);

        // Act
        MvcResult response = mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/internal/trust/protected-verification-submissions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(asJsonString(requestDto))
            )
            .andReturn();

        // Assert
        assertThat(response.getResponse().getStatus()).isEqualTo(HttpStatus.OK.value());
        var dto = objectMapper.readValue(
            response.getResponse().getContentAsString(),
            ProtectedVerificationSubmissionDto.class
        );
        assertThat(dto).isNotNull();
        assertThat(dto.partnerId()).isEqualTo(BusinessEntityTestData.DEFAULT_ENTITY);
    }

    @Test
    @WithExtendedJeapAuthenticationToken(
        subject = "deadbeef-deaf-0000-0000-000000000000",
        bpRoles = "deadbeef-deaf-0000-0000-000000000000 = ti_@protectedverificationsubmission_#write"
    )
    void testCreateProtectedVerificationSubmission_partnerNotTrusted_returnsForbidden() throws Exception {
        // Arrange - ENTITY_B (deadbeef-deaf-...) is not trusted in BusinessEntityTestData
        var requestDto = protectedVerificationSubmissionRequestDto(BusinessEntityTestData.ENTITY_B);

        // Act & Assert
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/internal/trust/protected-verification-submissions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(asJsonString(requestDto))
            )
            .andExpect(status().isForbidden());
    }

    private String asJsonString(Object object) throws JacksonException {
        return new JsonMapper().writeValueAsString(object);
    }
}
