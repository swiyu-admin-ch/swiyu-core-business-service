package ch.admin.bj.swiyu.core.business.modules.trust.infrastructure.web;

import static ch.admin.bj.swiyu.core.business.test.VcSchemaSubmissionTestData.*;
import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.bj.swiyu.core.business.modules.trust.api.VcSchemaSubmissionDto;
import ch.admin.bj.swiyu.core.business.test.TestRepositories;
import ch.admin.bj.swiyu.core.business.test.WithExtendedJeapAuthenticationToken;
import ch.admin.bj.swiyu.core.business.test.container.WithAllTestContainerInitializers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.UnsupportedEncodingException;
import java.util.List;
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
class VcSchemaSubmissionsInternalControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestRepositories repos;

    @Test
    @WithExtendedJeapAuthenticationToken(userRoles = { "ti_@vcschemasubmission_#read" })
    void testGetVcSchemaSubmission_withValidRole_returnsDto() throws Exception {
        // GIVEN
        var vcSchemaSubmission1 = vcSchemaSubmission(PARTNER_ID_1);
        var vcSchemaSubmission2 = vcSchemaSubmission(PARTNER_ID_2);
        repos.vcSchemaSubmission.saveAll(List.of(vcSchemaSubmission1, vcSchemaSubmission2));

        // WHEN
        var result = mockMvc
            .perform(
                MockMvcRequestBuilders.get(
                    "/api/v1/internal/trust/vc-schema-submissions/" + vcSchemaSubmission1.getId().toString()
                ).accept(MediaType.APPLICATION_JSON)
            )
            .andReturn();

        // THEN
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.OK.value());
        var dto = readVcSchemaSubmissionDto(result);
        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(vcSchemaSubmission1.getId());
        repos.vcSchemaSubmission.deleteAllInBatch();
    }

    @Test
    @WithExtendedJeapAuthenticationToken(userRoles = { "someOtherRole" })
    void testGetVcSchemaSubmission_withoutValidRole_forbidden() throws Exception {
        // GIVEN
        var vcSchemaSubmission = vcSchemaSubmission(PARTNER_ID_1);
        repos.vcSchemaSubmission.save(vcSchemaSubmission);

        // WHEN
        var result = mockMvc
            .perform(
                MockMvcRequestBuilders.get(
                    "/api/v1/internal/trust/vc-schema-submissions/" + vcSchemaSubmission.getId()
                ).accept(MediaType.APPLICATION_JSON)
            )
            .andReturn();
        // THEN
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        repos.vcSchemaSubmission.deleteAllInBatch(); // shouldn't be anything in here, but just to make sure
    }

    private VcSchemaSubmissionDto readVcSchemaSubmissionDto(MvcResult result)
        throws UnsupportedEncodingException, JsonProcessingException {
        var jsonResponse = result.getResponse().getContentAsString();
        return objectMapper.readValue(jsonResponse, VcSchemaSubmissionDto.class);
    }
}
