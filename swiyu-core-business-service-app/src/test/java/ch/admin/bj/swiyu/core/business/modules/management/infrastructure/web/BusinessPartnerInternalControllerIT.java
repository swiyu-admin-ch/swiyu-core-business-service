package ch.admin.bj.swiyu.core.business.modules.management.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.admin.bit.jeap.security.test.WithJeapAuthenticationToken;
import ch.admin.bj.swiyu.core.business.common.api.BusinessPartnerTypeDto;
import ch.admin.bj.swiyu.core.business.common.audit.AuditPublisher;
import ch.admin.bj.swiyu.core.business.modules.management.api.BusinessEntityDto;
import ch.admin.bj.swiyu.core.business.modules.management.api.CreateBusinessEntityDto;
import ch.admin.bj.swiyu.core.business.modules.management.api.UpdateBusinessEntityDto;
import ch.admin.bj.swiyu.core.business.modules.management.domain.pams.PamsClient;
import ch.admin.bj.swiyu.core.business.test.RestResponsePage;
import ch.admin.bj.swiyu.core.business.test.container.WithAllTestContainerInitializers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * For JeapAuthanticationToken see the <a href="https://bitbucket.bit.admin.ch/projects/JEAP/repos/jeap-spring-boot-starters/browse/jeap-spring-boot-security-starter-test">the README examples</a>
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@WithAllTestContainerInitializers
@MockitoBean(types = AuditPublisher.class)
class BusinessPartnerInternalControllerIT {

    static final String BUSINESS_ENTITY_MANAGEMENT_BASE_URL = "/api/v1/management/business-entities/";

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PamsClient pamsClient;

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "/delete_business_entities.sql")
    @WithJeapAuthenticationToken(bpRoles = "deadbeef-0000-0000-0000-000000000000 = ti_@businesspartner_#write")
    void testCreateBusinessEntity_newUser_thenSuccess() throws Exception {
        var response = callCreateBusinessEntity();
        assertThat(response.getResponse().getStatus()).isEqualTo(HttpStatus.OK.value());
        var businessEntityDto = toBusinessEntityDto(response.getResponse().getContentAsString());
        assertThat(businessEntityDto).isNotNull();
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "/delete_business_entities.sql")
    @WithJeapAuthenticationToken(bpRoles = "deadbeef-0000-0000-0000-000000000000 = ti_@businesspartner_#write")
    void testCreateBusinessEntity_governmentalType_thenFailure() throws Exception {
        callCreateBusinessEntity("Hello World", "test@test.local", BusinessPartnerTypeDto.GOVERNMENTAL_INSTITUTION)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("resource_forbidden"));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "/delete_business_entities.sql")
    @WithJeapAuthenticationToken(bpRoles = "40f377d5-cd11-458a-944b-0c4f73f2ddaa = ti_@businesspartner_#write")
    void testCreateBusinessEntity_governmentalType_thenSuccess() throws Exception {
        var response = callCreateBusinessEntity(
            "Hello World",
            "test@test.local",
            BusinessPartnerTypeDto.GOVERNMENTAL_INSTITUTION
        )
            .andExpect(status().isOk())
            .andReturn();
        var businessEntityDto = toBusinessEntityDto(response.getResponse().getContentAsString());
        assertThat(businessEntityDto).isNotNull();
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "/delete_business_entities.sql")
    @WithJeapAuthenticationToken
    void testCreateBusinessEntity_newUser_longName_thenBadRequest() throws Exception {
        callCreateBusinessEntity(
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas elementum, lectus sed rhoncus semper, libero arcu suscipit nibh, id cursus tortor",
            "lorem.ipsum@example.com",
            null
        ).andExpect(status().isBadRequest());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "/delete_business_entities.sql")
    @WithJeapAuthenticationToken
    void testCreateBusinessEntity_newUser_specialCharacters_thenBadRequest() throws Exception {
        callCreateBusinessEntity("Hello\u007FWorld", "utf8@example.com", null).andExpect(status().isBadRequest());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "/delete_business_entities.sql")
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "/insert_test_business_entities.sql")
    @WithJeapAuthenticationToken(bpRoles = "deadbeef-0000-0000-0000-000000000000 = ti_@businesspartner_#read")
    void testGetBusinessEntity_single_thenSuccess() throws Exception {
        var businessEntities = objectMapper.readValue(
            mockMvc
                .perform(MockMvcRequestBuilders.get(BUSINESS_ENTITY_MANAGEMENT_BASE_URL))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            new TypeReference<RestResponsePage<BusinessEntityDto>>() {}
        );
        assertThat(businessEntities.getMetadata().totalElements()).isEqualTo(1);
        assertThat(businessEntities.getContent()).hasSize(1);
        var dbInsertedBusinessEntity = businessEntities.getContent().get(0);
        assertThat(dbInsertedBusinessEntity.id()).isEqualTo(UUID.fromString("deadbeef-0000-0000-0000-000000000000"));
        assertThat(dbInsertedBusinessEntity.name()).isEqualTo("Hello World AG");
        assertThat(dbInsertedBusinessEntity.contactEmailAddress()).isEqualTo("hello.world@example.com");
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "/delete_business_entities.sql")
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "/insert_test_business_entities.sql")
    @WithJeapAuthenticationToken(
        bpRoles = {
            "deadbeef-0000-0000-0000-000000000000 = ti_@businesspartner_#read",
            "deadbeef-deaf-0000-0000-000000000000 = ti_@businesspartner_#read",
            "deadbeef-deaf-beef-0000-000000000000 = ti_@businesspartner_#read",
            "otherbp = otherRole",
        }
    )
    void testListBusinessPartners_thenSuccess() throws Exception {
        var businessEntitiesPage = objectMapper.readValue(
            mockMvc
                .perform(MockMvcRequestBuilders.get(BUSINESS_ENTITY_MANAGEMENT_BASE_URL))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            new TypeReference<RestResponsePage<BusinessEntityDto>>() {}
        );
        assertThat(businessEntitiesPage.getMetadata().totalElements()).isEqualTo(3);
        businessEntitiesPage
            .getContent()
            .forEach(businessEntity ->
                assertThat(businessEntity.id()).isIn(
                    UUID.fromString("deadbeef-0000-0000-0000-000000000000"),
                    UUID.fromString("deadbeef-deaf-0000-0000-000000000000"),
                    UUID.fromString("deadbeef-deaf-beef-0000-000000000000")
                )
            );
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "/delete_business_entities.sql")
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "/insert_test_business_entities.sql")
    @WithJeapAuthenticationToken(bpRoles = "deadbeef-aaaa-bbbb-cccc-000000000000 = ti_@businesspartner_#read")
    void testListBusinessPartners_none_thenSuccess() throws Exception {
        var businessEntities = objectMapper.readValue(
            mockMvc
                .perform(MockMvcRequestBuilders.get(BUSINESS_ENTITY_MANAGEMENT_BASE_URL))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            new TypeReference<RestResponsePage<BusinessEntityDto>>() {}
        );
        assertThat(businessEntities.getContent()).isEmpty();
        assertThat(businessEntities.getMetadata().totalElements()).isZero();
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "/delete_business_entities.sql")
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "/insert_test_business_entities.sql")
    @WithJeapAuthenticationToken
    void testListBusinessPartners_noJeapToken_thenForbidden() throws Exception {
        mockMvc
            .perform(MockMvcRequestBuilders.get(BUSINESS_ENTITY_MANAGEMENT_BASE_URL))
            .andExpect(status().isForbidden());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "/delete_business_entities.sql")
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "/insert_test_business_entities.sql")
    @WithJeapAuthenticationToken(bpRoles = "deadbeef-aaaa-bbbb-0000-000000000000 = ti_@businesspartner_#write")
    void testUpdateBusinessEntity_notExist_thenNotFound() throws Exception {
        var updateDto = new UpdateBusinessEntityDto("example name", "hello.hello.world@example.com");
        mockMvc
            .perform(
                MockMvcRequestBuilders.put(
                    BUSINESS_ENTITY_MANAGEMENT_BASE_URL + "{foreignId}",
                    "deadbeef-aaaa-bbbb-0000-000000000000"
                )
                    .content(objectMapper.writeValueAsString(updateDto))
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isNotFound());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "/delete_business_entities.sql")
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "/insert_test_business_entities.sql")
    @WithJeapAuthenticationToken(bpRoles = "deadbeef-deaf-0000-0000-000000000000 = ti_@businesspartner_#write")
    void testUpdateBusinessEntity_accessOtherEntity_thenForbidden() throws Exception {
        var updateDto = new UpdateBusinessEntityDto("example name", "hello.hello.world@example.com");
        mockMvc
            .perform(
                MockMvcRequestBuilders.put(
                    BUSINESS_ENTITY_MANAGEMENT_BASE_URL + "{foreignId}",
                    "deadbeef-0000-0000-0000-000000000000"
                )
                    .content(objectMapper.writeValueAsString(updateDto))
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isForbidden());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "/delete_business_entities.sql")
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "/insert_test_business_entities.sql")
    @WithJeapAuthenticationToken(bpRoles = "deadbeef-0000-0000-0000-000000000000 = ti_@businesspartner_#write")
    void testUpdateBusinessEntity_thenSuccess() throws Exception {
        var updateDto = new UpdateBusinessEntityDto("example name", "hello.hello.world@example.com");
        mockMvc
            .perform(
                MockMvcRequestBuilders.put(
                    BUSINESS_ENTITY_MANAGEMENT_BASE_URL + "{foreignId}",
                    "deadbeef-0000-0000-0000-000000000000"
                )
                    .content(objectMapper.writeValueAsString(updateDto))
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk());
    }

    private MvcResult callCreateBusinessEntity() throws Exception {
        return callCreateBusinessEntity(
            "Hello World AG",
            "hello.world@example.com",
            BusinessPartnerTypeDto.BUSINESS
        ).andReturn();
    }

    private ResultActions callCreateBusinessEntity(String name, String contact, BusinessPartnerTypeDto type)
        throws Exception {
        CreateBusinessEntityDto createBusinessEntityDto = new CreateBusinessEntityDto(name, contact, type);
        return mockMvc.perform(
            MockMvcRequestBuilders.post(BUSINESS_ENTITY_MANAGEMENT_BASE_URL)
                .content(objectMapper.writeValueAsString(createBusinessEntityDto))
                .contentType(MediaType.APPLICATION_JSON)
        );
    }

    private BusinessEntityDto toBusinessEntityDto(String responseString) throws JsonProcessingException {
        return objectMapper.readValue(responseString, BusinessEntityDto.class);
    }
}
