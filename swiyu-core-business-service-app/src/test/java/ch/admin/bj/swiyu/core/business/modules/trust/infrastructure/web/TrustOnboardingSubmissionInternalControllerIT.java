package ch.admin.bj.swiyu.core.business.modules.trust.infrastructure.web;

import static ch.admin.bj.swiyu.core.business.test.TrustOnboardingSubmissionTestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.admin.bj.swiyu.core.business.modules.trust.api.TrustOnboardingSubmissionDto;
import ch.admin.bj.swiyu.core.business.modules.trust.api.TrustOnboardingSubmissionListItemDto;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustOnboardingSubmission;
import ch.admin.bj.swiyu.core.business.test.RestResponsePage;
import ch.admin.bj.swiyu.core.business.test.TestRepositories;
import ch.admin.bj.swiyu.core.business.test.WithExtendedJeapAuthenticationToken;
import ch.admin.bj.swiyu.core.business.test.container.WithAllTestContainerInitializers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@WithAllTestContainerInitializers
class TrustOnboardingSubmissionInternalControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestRepositories testRepositories;

    @BeforeEach
    void setUp() {
        testRepositories.partnerDocuments.deleteAllInBatch();
        testRepositories.trustOnboardingSubmission.deleteAll();
    }

    @Test
    @WithExtendedJeapAuthenticationToken(
        subject = "deadbeef-0000-0000-0000-000000000000",
        bpRoles = "deadbeef-0000-0000-0000-000000000000 = ti_@trustonboardingsubmission_#read"
    )
    void testGetTrustOnboardings() throws Exception {
        // Arrange
        testRepositories.trustOnboardingSubmission.save(
            trustOnboardingSubmission(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                UUID.fromString("deadbeef-0000-0000-0000-000000000000")
            )
        );
        testRepositories.trustOnboardingSubmission.save(
            trustOnboardingSubmission(
                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                UUID.fromString("aaaaaaaa-0000-0000-0000-000000000000")
            )
        );

        // Act
        var onboardings = objectMapper.readValue(
            mockMvc
                .perform(MockMvcRequestBuilders.get("/api/v1/internal/trust/trust-onboarding-submission"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            new TypeReference<RestResponsePage<TrustOnboardingSubmissionListItemDto>>() {}
        );

        // Assert
        assertThat(onboardings.getContent()).hasSize(1);
        assertThat(onboardings.getContent().getFirst().id().toString()).hasToString(
            "00000000-0000-0000-0000-000000000001"
        );
    }

    @Test
    @WithExtendedJeapAuthenticationToken(
        subject = "deadbeef-0000-0000-0000-000000000000",
        userRoles = "ti_@trustonboardingsubmission_#read"
    )
    void testGetTrustOnboardings_WithAccessToAllPartners() throws Exception {
        // Arrange
        testRepositories.trustOnboardingSubmission.save(
            trustOnboardingSubmission(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                UUID.fromString("deadbeef-0000-0000-0000-000000000000")
            )
        );
        testRepositories.trustOnboardingSubmission.save(
            trustOnboardingSubmission(
                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                UUID.fromString("aaaaaaaa-0000-0000-0000-000000000000")
            )
        );

        // Act
        var onboardings = objectMapper.readValue(
            mockMvc
                .perform(MockMvcRequestBuilders.get("/api/v1/internal/trust/trust-onboarding-submission"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            new TypeReference<RestResponsePage<TrustOnboardingSubmissionListItemDto>>() {}
        );

        // Assert
        assertThat(onboardings.getContent()).hasSize(2);
    }

    @Test
    @WithExtendedJeapAuthenticationToken(
        subject = "deadbeef-0000-0000-0000-000000000000",
        bpRoles = "deadbeef-0000-0000-0000-000000000000 = ti_@trustonboardingsubmission_#read"
    )
    void testGetTrustOnboardingSubmission() throws Exception {
        // Arrange
        var submission = testRepositories.trustOnboardingSubmission.save(trustOnboardingSubmission());

        // Act
        var result = mockMvc
            .perform(
                MockMvcRequestBuilders.get(
                    "/api/v1/internal/trust/trust-onboarding-submission/{id}",
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
        bpRoles = "deadbeef-0000-0000-0000-000000000000 = ti_@trustonboardingsubmission_#write"
    )
    void testUpdateTrustOnboardingSubmission() throws Exception {
        // Arrange
        var testSubmission = trustOnboardingSubmission();
        TrustOnboardingSubmission submission = testRepositories.trustOnboardingSubmission.save(testSubmission);
        var trustOnboardingSubmissionId = submission.getId();
        var updateDto = trustOnboardingSubmissionRequestDtoUpdate();

        // Act
        MvcResult result = mockMvc
            .perform(
                MockMvcRequestBuilders.put(
                    "/api/v1/internal/trust/trust-onboarding-submission/{id}",
                    trustOnboardingSubmissionId
                )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(asJsonString(updateDto))
            )
            .andReturn();

        // Assert
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.OK.value());
        var trustOnboardingSubmissionDto = toTrustOnboardingSubmissionDto(result.getResponse().getContentAsString());
        assertThat(trustOnboardingSubmissionDto.entityEmail()).isEqualTo(updateDto.getEntityEmail());
    }

    @Test
    @WithExtendedJeapAuthenticationToken(
        subject = "deadbeef-0000-0000-0000-000000000000",
        bpRoles = "deadbeef-0000-0000-0000-000000000000 = ti_@trustonboardingsubmission_#write"
    )
    void testCreateTrustOnboardingSubmission() throws Exception {
        // Arrange
        var requestDto = trustOnboardingSubmissionRequestDto();

        // Act
        MvcResult response = mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/internal/trust/trust-onboarding-submission")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(asJsonString(requestDto))
            )
            .andReturn();

        // Assert
        // Add assertions to verify the response content
        assertThat(response.getResponse().getStatus()).isEqualTo(HttpStatus.OK.value());
        var trustOnboardingSubmissionDto = toTrustOnboardingSubmissionDto(response.getResponse().getContentAsString());
        assertThat(trustOnboardingSubmissionDto).isNotNull();
    }

    @Test
    @WithExtendedJeapAuthenticationToken(
        subject = "deadbeef-0000-0000-0000-000000000000",
        bpRoles = "deadbeef-0000-0000-0000-000000000000 = ti_@trustonboardingsubmission_#write"
    )
    void testCreateTrustOnboardingSubmission_invalidContactPhone_returnsBadRequest() throws Exception {
        var requestDto = trustOnboardingSubmissionRequestDto();
        var requestBody = (ObjectNode) objectMapper.readTree(asJsonString(requestDto));
        ((ObjectNode) requestBody.get("contactPerson")).put("phone", "0791234567");
        var invalidRequestBody = objectMapper.writeValueAsString(requestBody);

        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/internal/trust/trust-onboarding-submission")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidRequestBody)
            )
            .andExpect(status().isBadRequest());
    }

    private String asJsonString(Object object) throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(object);
    }

    private TrustOnboardingSubmissionDto toTrustOnboardingSubmissionDto(String responseString)
        throws JsonProcessingException {
        return objectMapper.readValue(responseString, TrustOnboardingSubmissionDto.class);
    }
}
