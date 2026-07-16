package ch.admin.bj.swiyu.core.business.modules.status.infrastructure.web;

import static ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData.*;
import static ch.admin.bj.swiyu.core.business.test.IdentifierTestData.identifierEntry_Initialized;
import static ch.admin.bj.swiyu.core.business.test.StatusTestData.VALID_STATUS_LIST_ISSUER_A_DID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.admin.bit.jeap.security.test.WithJeapAuthenticationToken;
import ch.admin.bj.swiyu.core.business.common.audit.AuditPublisher;
import ch.admin.bj.swiyu.core.business.common.utils.testSpan.WithTestSpan;
import ch.admin.bj.swiyu.core.business.modules.identifier.domain.IdentifierEntryRepository;
import ch.admin.bj.swiyu.core.business.modules.status.api.StatusListEntryCreationDto;
import ch.admin.bj.swiyu.core.business.modules.status.api.StatusListEntryDto;
import ch.admin.bj.swiyu.core.business.modules.status.service.StatusListEntryService;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.publisher.DomainEventPublisher;
import ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData;
import ch.admin.bj.swiyu.core.business.test.RestResponsePage;
import ch.admin.bj.swiyu.core.business.test.StatusTestData;
import ch.admin.bj.swiyu.core.business.test.container.WithAllTestContainerInitializers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * For JeapAuthanticationToken see the <a href="https://bitbucket.bit.admin.ch/projects/JEAP/repos/jeap-spring-boot-starters/browse/jeap-spring-boot-security-starter-test"> README examples</a>
 */
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
@EmbeddedKafka
@WithAllTestContainerInitializers
@TestMethodOrder(MethodOrderer.MethodName.class)
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "/delete_business_entities.sql")
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "/insert_test_business_entities.sql")
class StatusB2BControllerIT {

    static final String BASE_URL = "/api/v1/status/";

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    AuditPublisher auditPublisher;

    @MockitoBean
    DomainEventPublisher domainEventPublisher;

    @Autowired
    IdentifierEntryRepository identifierEntryRepository;

    @Autowired
    StatusListEntryService statusListEntryService;

    @Test
    @WithJeapAuthenticationToken(bpRoles = { BusinessEntityTestData.DEFAULT_ENTITY_S + " = ti_@status_#write" })
    void createStatusListEntry_authorized() throws Exception {
        // WHEN
        var result = objectMapper.readValue(
            mockMvc
                .perform(
                    MockMvcRequestBuilders.post(
                        BASE_URL +
                            "business-entities/" +
                            BusinessEntityTestData.DEFAULT_ENTITY_S +
                            "/status-list-entries/"
                    )
                )
                .andReturn()
                .getResponse()
                .getContentAsString(),
            StatusListEntryCreationDto.class
        );
        // THEN
        assertThat(result).isNotNull();
        assertThat(result.id()).isNotNull();
    }

    @Test
    @WithJeapAuthenticationToken(
        bpRoles = { BusinessEntityTestData.DEFAULT_ENTITY_S + " = ti_@businesspartner_#write" }
    )
    @WithTestSpan
    void createStatusListEntry_unauthorized() throws Exception {
        // GIVEN (BusinessEntity provided through SQL)
        statusListEntryService.createStatusListEntry(ENTITY_A).id();
        // WHEN
        var result = mockMvc
            .perform(
                MockMvcRequestBuilders.post(
                    BASE_URL + "business-entities/" + BusinessEntityTestData.DEFAULT_ENTITY_S + "/status-list-entries/"
                )
            )
            .andReturn()
            .getResponse();
        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(403);
    }

    @Test
    @WithJeapAuthenticationToken(
        bpRoles = {
            BusinessEntityTestData.ENTITY_A_S + " = ti_@businesspartner_#write",
            BusinessEntityTestData.ENTITY_A_S + " = ti_@status_#write",
        }
    )
    @WithTestSpan
    void createStatusListEntry_wrongAuthorized() throws Exception {
        // GIVEN (BusinessEntity provided through SQL)
        statusListEntryService.createStatusListEntry(ENTITY_A);
        // WHEN
        var result = mockMvc
            .perform(
                MockMvcRequestBuilders.post(
                    BASE_URL + "business-entities/" + BusinessEntityTestData.ENTITY_B_S + "/status-list-entries/"
                )
            )
            .andReturn()
            .getResponse();
        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(403);
    }

    @Test
    @WithJeapAuthenticationToken(bpRoles = { BusinessEntityTestData.DEFAULT_ENTITY_S + " = ti_@status_#write" })
    @WithTestSpan
    void updateStatusListEntry_authorized() throws Exception {
        // GIVEN (BusinessEntity provided through SQL)
        identifierEntryRepository.save(identifierEntry_Initialized(DEFAULT_ENTITY, VALID_STATUS_LIST_ISSUER_A_DID));
        var statusListEntryId = statusListEntryService.createStatusListEntry(DEFAULT_ENTITY).id();

        // WHEN
        var result = mockMvc
            .perform(
                MockMvcRequestBuilders.put(
                    BASE_URL +
                        "business-entities/" +
                        BusinessEntityTestData.DEFAULT_ENTITY_S +
                        "/status-list-entries/" +
                        statusListEntryId
                )
                    .content(StatusTestData.VALID_STATUS_LIST_VC_FROM_ISSUER_A)
                    .contentType("application/statuslist+jwt")
            )
            .andReturn()
            .getResponse();
        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(200);
    }

    @Test
    @WithJeapAuthenticationToken(bpRoles = { BusinessEntityTestData.DEFAULT_ENTITY_S + " = ti_@status_#write" })
    @WithTestSpan
    void updateStatusListEntry_authorized_throwsDidNotResolvable() throws Exception {
        // GIVEN (BusinessEntity provided through SQL)
        identifierEntryRepository.save(identifierEntry_Initialized(DEFAULT_ENTITY, VALID_STATUS_LIST_ISSUER_A_DID));
        var statusListEntryId = statusListEntryService.createStatusListEntry(DEFAULT_ENTITY).id();

        // WHEN
        mockMvc
            .perform(
                MockMvcRequestBuilders.put(
                    BASE_URL +
                        "business-entities/" +
                        BusinessEntityTestData.DEFAULT_ENTITY_S +
                        "/status-list-entries/" +
                        statusListEntryId
                )
                    .content(StatusTestData.INVALID_STATUS_LIST_WITH_INVALID_NON_JSON_JWT_PAYLOAD)
                    .contentType("application/statuslist+jwt")
            )
            // THEN
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("status_list_validation_failed"))
            .andExpect(jsonPath("$.message").value("Provided status list resource is invalid."))
            .andExpect(jsonPath("$.additionalDetails").isArray())
            .andExpect(jsonPath("$.additionalDetails.length()").value(Matchers.greaterThan(1)));
    }

    @Test
    @WithJeapAuthenticationToken(bpRoles = { BusinessEntityTestData.UNKNOWN_ENTITY_S + " = ti_@status_#write" })
    void createStatusListEntry_NotExistingBusinessPartner() throws Exception {
        // WHEN
        mockMvc
            .perform(
                MockMvcRequestBuilders.post(
                    BASE_URL + "business-entities/" + BusinessEntityTestData.UNKNOWN_ENTITY_S + "/status-list-entries/"
                )
                    .content(StatusTestData.VALID_STATUS_LIST_VC_FROM_ISSUER_A)
                    .contentType("application/statuslist+jwt")
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("The business partner does not exist in this environment."));
    }

    @Test
    @WithJeapAuthenticationToken(bpRoles = { BusinessEntityTestData.DEFAULT_ENTITY_S + " = ti_@status_#read" })
    @WithTestSpan
    void updateStatusListEntry_unauthorized() throws Exception {
        // GIVEN (BusinessEntity provided through SQL)
        var statusListEntryId = statusListEntryService.createStatusListEntry(DEFAULT_ENTITY).id();
        // WHEN
        var result = mockMvc
            .perform(
                MockMvcRequestBuilders.put(
                    BASE_URL +
                        "business-entities/" +
                        BusinessEntityTestData.DEFAULT_ENTITY_S +
                        "/status-list-entries/" +
                        statusListEntryId
                )
                    .content(StatusTestData.VALID_STATUS_LIST_VC_FROM_ISSUER_A)
                    .contentType("application/statuslist+jwt")
            )
            .andReturn()
            .getResponse();
        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(403);
    }

    @Test
    @WithJeapAuthenticationToken(bpRoles = { BusinessEntityTestData.ENTITY_A_S + " = ti_@status_#write" })
    @WithTestSpan
    void updateStatusListEntry_wrongAuthorized() throws Exception {
        // GIVEN (BusinessEntity provided through SQL)
        var statusListEntryId = statusListEntryService.createStatusListEntry(ENTITY_B).id();
        // WHEN
        var result = mockMvc
            .perform(
                MockMvcRequestBuilders.put(
                    BASE_URL +
                        "business-entities/" +
                        BusinessEntityTestData.ENTITY_A_S +
                        "/status-list-entries/" +
                        statusListEntryId
                )
                    .content(StatusTestData.VALID_STATUS_LIST_VC_FROM_ISSUER_A)
                    .contentType("application/statuslist+jwt")
            )
            .andReturn()
            .getResponse();
        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(404);
    }

    @Test
    @WithJeapAuthenticationToken(bpRoles = { BusinessEntityTestData.ENTITY_A_S + " = ti_@status_#read" })
    @WithTestSpan
    void getAllStatusListEntries_authorized() throws Exception {
        // GIVEN (BusinessEntity provided through SQL)
        var statusListEntryId01 = statusListEntryService.createStatusListEntry(ENTITY_A).id();
        var statusListEntryId02 = statusListEntryService.createStatusListEntry(ENTITY_A).id();
        statusListEntryService.createStatusListEntry(ENTITY_B);
        // WHEN
        var result = objectMapper.readValue(
            mockMvc
                .perform(
                    MockMvcRequestBuilders.get(
                        BASE_URL + "business-entities/" + BusinessEntityTestData.ENTITY_A_S + "/status-list-entries/"
                    )
                )
                .andReturn()
                .getResponse()
                .getContentAsString(),
            new TypeReference<RestResponsePage<StatusListEntryDto>>() {}
        );
        // THEN
        assertThat(result).isNotNull();
        // 3. status list should not be visible as it belongs to another business entity
        assertThat(result.getMetadata().totalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);

        // inverted order of occurrence as we default sort by updatedAt
        var item01 = result.getContent().get(0);
        assertThat(item01.id()).isEqualTo(statusListEntryId02);
        var item02 = result.getContent().get(1);
        assertThat(item02.id()).isEqualTo(statusListEntryId01);
    }

    @Test
    @WithJeapAuthenticationToken(bpRoles = { BusinessEntityTestData.ENTITY_A_S + " = ti_@status_#write" })
    @WithTestSpan
    void getAllStatusListEntries_unauthorized() throws Exception {
        // GIVEN (BusinessEntity provided through SQL)
        statusListEntryService.createStatusListEntry(ENTITY_B);
        // WHEN
        var result = mockMvc
            .perform(
                MockMvcRequestBuilders.get(
                    BASE_URL + "business-entities/" + BusinessEntityTestData.ENTITY_B_S + "/status-list-entries/"
                )
            )
            .andReturn()
            .getResponse();
        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(403);
    }
}
