package ch.admin.bj.swiyu.core.business.modules.management.infrastructure.web;

import static ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData.insertTestBusinessPartners;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.admin.bit.jeap.security.test.WithJeapAuthenticationToken;
import ch.admin.bj.swiyu.core.business.common.api.ApiObjectDto;
import ch.admin.bj.swiyu.core.business.common.api.BusinessPartnerTypeDto;
import ch.admin.bj.swiyu.core.business.common.api.ObjectLimitsDto;
import ch.admin.bj.swiyu.core.business.common.audit.AuditPublisher;
import ch.admin.bj.swiyu.core.business.common.service.LocalizedMapUtil;
import ch.admin.bj.swiyu.core.business.modules.identifier.service.IdentifierEntryService;
import ch.admin.bj.swiyu.core.business.modules.management.api.BusinessPartnerDto;
import ch.admin.bj.swiyu.core.business.modules.management.api.CreatePartnerDto;
import ch.admin.bj.swiyu.core.business.modules.management.domain.pams.PamsClient;
import ch.admin.bj.swiyu.core.business.modules.status.service.StatusListEntryService;
import ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData;
import ch.admin.bj.swiyu.core.business.test.RestResponsePage;
import ch.admin.bj.swiyu.core.business.test.TestRepositories;
import ch.admin.bj.swiyu.core.business.test.container.WithAllTestContainerInitializers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
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
 * For JeapAuthanticationToken see the <a href="https://bitbucket.bit.admin.ch/projects/JEAP/repos/jeap-spring-boot-starters/browse/jeap-spring-boot-security-starter-test"> README examples</a>
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@WithAllTestContainerInitializers
@MockitoBean(types = AuditPublisher.class)
class BusinessPartnerInternalV2ControllerIT {

    static final String BUSINESS_PARTNER_MANAGEMENT_BASE_URL = "/api/v2/internal/management/business-partners/";

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PamsClient pamsClient;

    @MockitoBean
    IdentifierEntryService identifierEntryService;

    @MockitoBean
    StatusListEntryService statusListEntryService;

    @Autowired
    TestRepositories repos;

    @BeforeEach
    void setUp() {
        repos.businessPartner.deleteAll();
        repos.businessPartner.flush();
        mockLimits();
    }

    @Test
    @WithJeapAuthenticationToken
    void testCreateBusinessPartner_newUser_thenSuccess() throws Exception {
        Mockito.when(identifierEntryService.createIdentifierEntry(Mockito.any())).thenReturn(null);

        var response = callCreateBusinessPartner();
        assertThat(response.getResponse().getStatus()).isEqualTo(HttpStatus.CREATED.value());
        var businessPartnerDto = toBusinessPartnerDto(response.getResponse().getContentAsString());
        assertThat(businessPartnerDto).isNotNull();
    }

    @Test
    @WithJeapAuthenticationToken(bpRoles = "deadbeef-0000-0000-0000-000000000000 = ti_@businesspartner_#write")
    void testCreateBusinessPartner_governmentalType_thenFailure() throws Exception {
        Mockito.when(identifierEntryService.createIdentifierEntry(Mockito.any())).thenReturn(null);

        callCreateBusinessPartner("Hello World", "test@test.local", BusinessPartnerTypeDto.GOVERNMENTAL_INSTITUTION)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("resource_forbidden"));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "/delete_business_entities.sql")
    @WithJeapAuthenticationToken(bpRoles = "40f377d5-cd11-458a-944b-0c4f73f2ddaa = ti_@businesspartner_#write")
    void testCreateBusinessPartner_governmentalType_thenSuccess() throws Exception {
        Mockito.when(identifierEntryService.createIdentifierEntry(Mockito.any())).thenReturn(null);
        var response = callCreateBusinessPartner(
            "Hello World",
            "test@test.local",
            BusinessPartnerTypeDto.GOVERNMENTAL_INSTITUTION
        )
            .andExpect(status().isCreated())
            .andReturn();
        var businessPartnerDto = toBusinessPartnerDto(response.getResponse().getContentAsString());
        assertThat(businessPartnerDto).isNotNull();
    }

    @Test
    @WithJeapAuthenticationToken
    void testCreateBusinessPartner_newUser_longName_thenBadRequest() throws Exception {
        callCreateBusinessPartner(
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas elementum, lectus sed rhoncus semper, libero arcu suscipit nibh, id cursus tortor",
            "lorem.ipsum@example.com",
            BusinessPartnerTypeDto.BUSINESS
        ).andExpect(status().isBadRequest());
    }

    @Test
    @WithJeapAuthenticationToken
    void testCreateBusinessPartner_validation_longName_containsFieldError() throws Exception {
        callCreateBusinessPartner(
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas elementum XXXXXXXXXXXX",
            "lorem.ipsum@example.com",
            BusinessPartnerTypeDto.BUSINESS
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("data_invalid"))
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.additionalDetails[0]").value("name: size must be between 0 and 45"));
    }

    @Test
    @WithJeapAuthenticationToken
    void testCreateBusinessPartner_validation_blankName_containsFieldError() throws Exception {
        callCreateBusinessPartner("   ", "valid@example.com", BusinessPartnerTypeDto.BUSINESS)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("data_invalid"))
            .andExpect(jsonPath("$.additionalDetails").isArray())
            .andExpect(jsonPath("$.additionalDetails", hasItem(containsString("name:"))));
    }

    @Test
    @WithJeapAuthenticationToken
    void testCreateBusinessPartner_validation_invalidPhone_containsFieldError() throws Exception {
        mockMvc
            .perform(
                MockMvcRequestBuilders.post(BUSINESS_PARTNER_MANAGEMENT_BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            new CreatePartnerDto(
                                "Valid Name",
                                BusinessPartnerTypeDto.BUSINESS,
                                null,
                                "Some Street",
                                "8000",
                                "Zurich",
                                "Switzerland",
                                "Zurich",
                                "not-a-phone-number",
                                "valid@example.com"
                            )
                        )
                    )
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("data_invalid"))
            .andExpect(jsonPath("$.additionalDetails", hasItem(containsString("contactPhone:"))));
    }

    @Test
    @WithJeapAuthenticationToken
    void testCreateBusinessPartner_validation_invalidEmail_containsFieldError() throws Exception {
        callCreateBusinessPartner("Valid Name", "not-an-email", BusinessPartnerTypeDto.BUSINESS)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("data_invalid"))
            .andExpect(jsonPath("$.additionalDetails", hasItem(containsString("contactEmail:"))));
    }

    @Test
    @WithJeapAuthenticationToken
    void testCreateBusinessPartner_validation_invalidUid_containsFieldError() throws Exception {
        mockMvc
            .perform(
                MockMvcRequestBuilders.post(BUSINESS_PARTNER_MANAGEMENT_BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            new CreatePartnerDto(
                                "Valid Name",
                                BusinessPartnerTypeDto.BUSINESS,
                                "INVALID-UID",
                                "Some Street",
                                "8000",
                                "Zurich",
                                "Switzerland",
                                "Zurich",
                                "+41791122334",
                                "valid@example.com"
                            )
                        )
                    )
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("data_invalid"))
            .andExpect(jsonPath("$.additionalDetails", hasItem(containsString("uid:"))));
    }

    @Test
    @WithJeapAuthenticationToken
    void testCreateBusinessPartner_validation_invalidZip_containsFieldError() throws Exception {
        mockMvc
            .perform(
                MockMvcRequestBuilders.post(BUSINESS_PARTNER_MANAGEMENT_BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            new CreatePartnerDto(
                                "Valid Name",
                                BusinessPartnerTypeDto.BUSINESS,
                                null,
                                "Some Street",
                                "99999",
                                "Zurich",
                                "Switzerland",
                                "Zurich",
                                "+41791122334",
                                "valid@example.com"
                            )
                        )
                    )
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("data_invalid"))
            .andExpect(jsonPath("$.additionalDetails", hasItem(containsString("addressZipCode:"))));
    }

    @Test
    @WithJeapAuthenticationToken
    void testCreateBusinessPartner_validation_multipleErrors_allFieldsReported() throws Exception {
        mockMvc
            .perform(
                MockMvcRequestBuilders.post(BUSINESS_PARTNER_MANAGEMENT_BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            new CreatePartnerDto(
                                "",
                                BusinessPartnerTypeDto.BUSINESS,
                                null,
                                null,
                                "bad-zip",
                                "",
                                null,
                                null,
                                "bad-phone",
                                "bad-email"
                            )
                        )
                    )
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("data_invalid"))
            .andExpect(jsonPath("$.additionalDetails").isArray())
            .andExpect(jsonPath("$.additionalDetails.length()").value(greaterThan(1)));
    }

    @Test
    @WithJeapAuthenticationToken
    void testCreateBusinessPartner_newUser_specialCharacters_thenBadRequest() throws Exception {
        callCreateBusinessPartner("Hello\u007FWorld", "utf8@example.com", BusinessPartnerTypeDto.BUSINESS).andExpect(
            status().isBadRequest()
        );
    }

    @Test
    @WithJeapAuthenticationToken(bpRoles = "deadbeef-0000-0000-0000-000000000000 = ti_@businesspartner_#read")
    void testGetBusinessPartners_thenSuccess() throws Exception {
        // GIVEN
        insertTestBusinessPartners(repos.businessPartner);
        repos.businessPartner.flush();
        // THEN
        var businessEntities = objectMapper.readValue(
            mockMvc
                .perform(MockMvcRequestBuilders.get(BUSINESS_PARTNER_MANAGEMENT_BASE_URL))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            new TypeReference<RestResponsePage<BusinessPartnerDto>>() {}
        );
        // THEN
        assertThat(businessEntities.getMetadata().totalElements()).isEqualTo(1);
        assertThat(businessEntities.getContent()).hasSize(1);
        var dbInsertedBusinessPartner = businessEntities.getContent().get(0);
        assertThat(dbInsertedBusinessPartner.id()).isEqualTo(UUID.fromString("deadbeef-0000-0000-0000-000000000000"));
        assertThat(dbInsertedBusinessPartner.name()).isEqualTo("Hello World AG");
        assertThat(LocalizedMapUtil.getDefaultValue(dbInsertedBusinessPartner.entityName())).isEqualTo(
            "Hello World AG"
        );
    }

    @Test
    @WithJeapAuthenticationToken(userRoles = "ti_@businesspartner_#read")
    void testGetBusinessPartners_WithAccessToAllPartners_thenSuccess() throws Exception {
        // GIVEN
        insertTestBusinessPartners(repos.businessPartner);
        repos.businessPartner.flush();
        // THEN
        var businessEntities = objectMapper.readValue(
            mockMvc
                .perform(MockMvcRequestBuilders.get(BUSINESS_PARTNER_MANAGEMENT_BASE_URL))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            new TypeReference<RestResponsePage<BusinessPartnerDto>>() {}
        );
        // THEN
        assertThat(businessEntities.getMetadata().totalElements()).isEqualTo(3);
    }

    @Test
    @WithJeapAuthenticationToken(
        bpRoles = {
            "deadbeef-0000-0000-0000-000000000000 = ti_@businesspartner_#read",
            "deadbeef-deaf-0000-0000-000000000000 = ti_@businesspartner_#read",
            "otherbp = otherRole",
        }
    )
    void testGetBusinessPartner_multi_thenSuccess() throws Exception {
        // GIVEN
        insertTestBusinessPartners(repos.businessPartner);
        repos.businessPartner.flush();
        // THEN
        var businessEntitiesPage = objectMapper.readValue(
            mockMvc
                .perform(MockMvcRequestBuilders.get(BUSINESS_PARTNER_MANAGEMENT_BASE_URL))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            new TypeReference<RestResponsePage<BusinessPartnerDto>>() {}
        );
        assertThat(businessEntitiesPage.getMetadata().totalElements()).isEqualTo(2);
        businessEntitiesPage
            .getContent()
            .forEach(businessPartner ->
                assertThat(businessPartner.id()).isIn(
                    UUID.fromString("deadbeef-0000-0000-0000-000000000000"),
                    UUID.fromString("deadbeef-deaf-0000-0000-000000000000")
                )
            );
    }

    @Test
    @WithJeapAuthenticationToken(bpRoles = "deadbeef-aaaa-bbbb-cccc-000000000000 = ti_@businesspartner_#read")
    void testGetBusinessPartner_none_thenSuccess() throws Exception {
        // GIVEN
        insertTestBusinessPartners(repos.businessPartner);
        repos.businessPartner.flush();
        // THEN
        var businessEntities = objectMapper.readValue(
            mockMvc
                .perform(MockMvcRequestBuilders.get(BUSINESS_PARTNER_MANAGEMENT_BASE_URL))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            new TypeReference<RestResponsePage<BusinessPartnerDto>>() {}
        );
        assertThat(businessEntities.getContent()).isEmpty();
        assertThat(businessEntities.getMetadata().totalElements()).isZero();
    }

    @Test
    @WithJeapAuthenticationToken
    void testGetBusinessPartner_noRole_thenIsForbidden() throws Exception {
        mockMvc
            .perform(MockMvcRequestBuilders.get(BUSINESS_PARTNER_MANAGEMENT_BASE_URL))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithJeapAuthenticationToken(bpRoles = BusinessEntityTestData.ENTITY_A_S + " = ti_@businesspartner_#read")
    void testGetBusinessPartner_byId_thenSuccess() throws Exception {
        // GIVEN
        insertTestBusinessPartners(repos.businessPartner);
        repos.businessPartner.flush();
        // THEN
        var businessPartnerDto = objectMapper.readValue(
            mockMvc
                .perform(
                    MockMvcRequestBuilders.get(BUSINESS_PARTNER_MANAGEMENT_BASE_URL + BusinessEntityTestData.ENTITY_A_S)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            BusinessPartnerDto.class
        );
        assertThat(businessPartnerDto.id()).isEqualTo(UUID.fromString(BusinessEntityTestData.ENTITY_A_S));
        assertThat(businessPartnerDto.name()).isEqualTo("Hello World AG");
        assertThat(LocalizedMapUtil.getDefaultValue(businessPartnerDto.entityName())).isEqualTo("Hello World AG");
    }

    @Test
    @WithJeapAuthenticationToken(bpRoles = "deadbeef-aaaa-bbbb-cccc-000000000000 = ti_@businesspartner_#read")
    void testGetBusinessPartner_byId_wrongPartner_thenForbidden() throws Exception {
        // GIVEN
        insertTestBusinessPartners(repos.businessPartner);
        repos.businessPartner.flush();

        // WHEN user tries to access a partner they do not manage (deadbeef-0000...)
        // THEN they should get Not Found (to hide existence) or Forbidden.
        // The controller throws ResourceNotFoundException, which typically maps to 404 but mapped to 400 by RestExceptionHandler.
        mockMvc
            .perform(
                MockMvcRequestBuilders.get(
                    BUSINESS_PARTNER_MANAGEMENT_BASE_URL + "deadbeef-0000-0000-0000-000000000000"
                )
            )
            .andExpect(status().isForbidden());
    }

    private void mockLimits() {
        var limits = ObjectLimitsDto.builder()
            .relatesTo(ApiObjectDto.IDENTIFIER_ENTRY)
            .currentCount(0L)
            .maxCount(100L)
            .build();
        Mockito.when(statusListEntryService.getCurrentLimits(Mockito.any())).thenReturn(limits);
        Mockito.when(identifierEntryService.getCurrentLimits(Mockito.any())).thenReturn(limits);
    }

    private MvcResult callCreateBusinessPartner() throws Exception {
        return callCreateBusinessPartner(
            "Hello World AG",
            "hello.world@example.com",
            BusinessPartnerTypeDto.BUSINESS
        ).andReturn();
    }

    private ResultActions callCreateBusinessPartner(String name, String contactEmail, BusinessPartnerTypeDto type)
        throws Exception {
        CreatePartnerDto createBusinessPartnerDto = new CreatePartnerDto(
            name,
            type,
            null,
            "Some street",
            "8000",
            "Zurich",
            "Switzerland",
            "Zurich",
            "+41791122334",
            contactEmail
        );
        return mockMvc.perform(
            MockMvcRequestBuilders.post(BUSINESS_PARTNER_MANAGEMENT_BASE_URL)
                .content(objectMapper.writeValueAsString(createBusinessPartnerDto))
                .contentType(MediaType.APPLICATION_JSON)
        );
    }

    private BusinessPartnerDto toBusinessPartnerDto(String responseString) throws JsonProcessingException {
        return objectMapper.readValue(responseString, BusinessPartnerDto.class);
    }
}
