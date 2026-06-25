package ch.admin.bj.swiyu.core.business.modules.trust.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import ch.admin.bj.swiyu.core.business.modules.management.domain.pams.PamsClient;
import ch.admin.bj.swiyu.core.business.modules.trust.api.CreateVcMetadataTypeDto;
import ch.admin.bj.swiyu.core.business.modules.trust.api.VcSchemaSubmissionDto;
import ch.admin.bj.swiyu.core.business.modules.trust.config.TrustRegistryProperties;
import ch.admin.bj.swiyu.core.business.test.VCTypeMetadataTestData;
import ch.admin.bj.swiyu.core.business.test.WithExtendedJeapAuthenticationToken;
import ch.admin.bj.swiyu.core.business.test.container.WithAllTestContainerInitializers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.MalformedURLException;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * For JeapAuthanticationToken see the <a href="https://bitbucket.bit.admin.ch/projects/JEAP/repos/jeap-spring-boot-starters/browse/jeap-spring-boot-security-starter-test"> README examples</a>
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@WithAllTestContainerInitializers
@EmbeddedKafka
public class VcSchemaSubmissionsB2BControllerIT {

    private static final String VC_SCHEMA_SUBMISSIONS_B2B_BASE_URL = "/api/v1/trust/";

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PamsClient pamsClient;

    @Autowired
    VCTypeMetadataTestData testData;

    @MockitoBean
    private TrustRegistryProperties trustRegistryProperties;

    @BeforeEach
    void setUp() throws MalformedURLException {
        when(trustRegistryProperties.dataServiceBaseUrl()).thenReturn(URI.create("https://test-url.ch").toURL());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "/delete_vc_schema_submissions.sql")
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "/delete_business_entities.sql")
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "/insert_test_business_entities.sql")
    @WithExtendedJeapAuthenticationToken(
        subject = "deadbeef-0000-0000-0000-000000000000",
        bpRoles = "deadbeef-0000-0000-0000-000000000000 = ti_@vcschemasubmission_#read, ti_@vcschemasubmission_#write"
    )
    void testCreateVcSchemaSubmission_thenSuccess() throws Exception {
        // GIVEN / WHEN
        var response = callCreateVcSchemaSubmission(testData.validTypeMetadata());
        // THEN
        assertThat(response.getResponse().getStatus()).isEqualTo(HttpStatus.OK.value());
        var businessEntityDto = toSubmitVcSchemaSubmissionDto(response.getResponse().getContentAsString());
        assertThat(businessEntityDto).isNotNull();
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "/delete_vc_schema_submissions.sql")
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "/delete_business_entities.sql")
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "/insert_test_business_entities.sql")
    @WithExtendedJeapAuthenticationToken(
        subject = "deadbeef-deaf-0000-0000-000000000000",
        bpRoles = "deadbeef-deaf-0000-0000-000000000000 = ti_@vcschemasubmission_#read, ti_@vcschemasubmission_#write"
    )
    void testCreateVcSchemaSubmission_thenFailure() throws Exception {
        // GIVEN / WHEN
        var response = callCreateVcSchemaSubmission(testData.validTypeMetadata());
        // THEN
        assertThat(response.getResponse().getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
    }

    private MvcResult callCreateVcSchemaSubmission(String vcTypeMetadata) throws Exception {
        CreateVcMetadataTypeDto createVcMetadataTypeDto = new CreateVcMetadataTypeDto(vcTypeMetadata);
        return mockMvc
            .perform(
                MockMvcRequestBuilders.post(VC_SCHEMA_SUBMISSIONS_B2B_BASE_URL + "vc-schema-submissions")
                    .content(objectMapper.writeValueAsString(createVcMetadataTypeDto))
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andReturn();
    }

    private VcSchemaSubmissionDto toSubmitVcSchemaSubmissionDto(String responseString) throws JsonProcessingException {
        return objectMapper.readValue(responseString, VcSchemaSubmissionDto.class);
    }
}
