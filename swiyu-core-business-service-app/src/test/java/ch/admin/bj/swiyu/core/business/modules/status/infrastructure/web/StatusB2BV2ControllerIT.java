package ch.admin.bj.swiyu.core.business.modules.status.infrastructure.web;

import static ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.admin.bit.jeap.security.test.WithJeapAuthenticationToken;
import ch.admin.bj.swiyu.core.business.common.utils.testSpan.WithTestSpan;
import ch.admin.bj.swiyu.core.business.modules.status.service.StatusListEntryService;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.publisher.DomainEventPublisher;
import ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData;
import ch.admin.bj.swiyu.core.business.test.StatusTestData;
import ch.admin.bj.swiyu.core.business.test.container.WithAllTestContainerInitializers;
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

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
@EmbeddedKafka
@WithAllTestContainerInitializers
@TestMethodOrder(MethodOrderer.MethodName.class)
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "/delete_business_entities.sql")
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "/insert_test_business_entities.sql")
class StatusB2BV2ControllerIT {

    static final String BASE_URL = "/api/v2/status/";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    DomainEventPublisher domainEventPublisher;

    @Autowired
    StatusListEntryService statusListEntryService;

    @Test
    @WithJeapAuthenticationToken(bpRoles = { BusinessEntityTestData.DEFAULT_ENTITY_S + " = ti_@status_#read" })
    @WithTestSpan
    void updateStatusListEntry_unauthorized() throws Exception {
        var statusListEntryId = statusListEntryService.createStatusListEntry(DEFAULT_ENTITY).id();

        var result = mockMvc
            .perform(
                MockMvcRequestBuilders.put(
                    BASE_URL + "business-entities/" + DEFAULT_ENTITY_S + "/status-list-entries/" + statusListEntryId
                )
                    .content(StatusTestData.VALID_STATUS_LIST_VC_FROM_ISSUER_A)
                    .contentType("application/statuslist+jwt")
            )
            .andReturn()
            .getResponse();

        assertThat(result.getStatus()).isEqualTo(403);
    }

    @Test
    @WithJeapAuthenticationToken(bpRoles = { BusinessEntityTestData.ENTITY_A_S + " = ti_@status_#write" })
    @WithTestSpan
    void updateStatusListEntry_wrongAuthorized() throws Exception {
        var statusListEntryId = statusListEntryService.createStatusListEntry(ENTITY_B).id();

        var result = mockMvc
            .perform(
                MockMvcRequestBuilders.put(
                    BASE_URL + "business-entities/" + ENTITY_A_S + "/status-list-entries/" + statusListEntryId
                )
                    .content(StatusTestData.VALID_STATUS_LIST_VC_FROM_ISSUER_A)
                    .contentType("application/statuslist+jwt")
            )
            .andReturn()
            .getResponse();

        assertThat(result.getStatus()).isEqualTo(404);
    }

    @Test
    @WithJeapAuthenticationToken(bpRoles = { BusinessEntityTestData.DEFAULT_ENTITY_S + " = ti_@status_#write" })
    @WithTestSpan
    void updateStatusListEntry_withV1Token_failsSwissProfileCheck() throws Exception {
        var statusListEntryId = statusListEntryService.createStatusListEntry(DEFAULT_ENTITY).id();

        mockMvc
            .perform(
                MockMvcRequestBuilders.put(
                    BASE_URL + "business-entities/" + DEFAULT_ENTITY_S + "/status-list-entries/" + statusListEntryId
                )
                    .content(StatusTestData.VALID_STATUS_LIST_VC_FROM_ISSUER_A)
                    .contentType("application/statuslist+jwt")
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("status_list_validation_failed"));
    }
}
