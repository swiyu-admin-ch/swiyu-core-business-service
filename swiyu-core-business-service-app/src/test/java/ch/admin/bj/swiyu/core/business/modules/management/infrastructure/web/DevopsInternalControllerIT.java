package ch.admin.bj.swiyu.core.business.modules.management.infrastructure.web;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.admin.bit.jeap.security.test.WithJeapAuthenticationToken;
import ch.admin.bj.swiyu.core.business.modules.management.domain.BusinessPartnerRepository;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.publisher.DomainEventPublisher;
import ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData;
import ch.admin.bj.swiyu.core.business.test.container.WithAllTestContainerInitializers;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The DevOps sync endpoints (EID-6988) re-publish the TiBusinessPartnerUpdatedEvent without
 * state change and may be called repeatedly.
 */
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
@EmbeddedKafka
@WithAllTestContainerInitializers
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "/delete_business_entities.sql")
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "/insert_test_business_entities.sql")
class DevopsInternalControllerIT {

    static final String BASE_URL = "/api/v1/internal/devops/business-partners";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    BusinessPartnerRepository businessPartnerRepository;

    @MockitoBean
    DomainEventPublisher domainEventPublisher;

    @Test
    @WithJeapAuthenticationToken(bpRoles = { BusinessEntityTestData.DEFAULT_ENTITY_S + " = ti_@businesspartner_#sync" })
    void syncSingle_publishesTheEventAndChangesNothing() throws Exception {
        var partnerBefore = businessPartnerRepository.findById(BusinessEntityTestData.DEFAULT_ENTITY).orElseThrow();

        mockMvc
            .perform(post(BASE_URL + "/sync/" + BusinessEntityTestData.DEFAULT_ENTITY_S))
            .andExpect(status().isNoContent());

        verify(domainEventPublisher).publishTiBusinessPartnerUpdatedEvent(
            argThat(event -> BusinessEntityTestData.DEFAULT_ENTITY.equals(event.getPayload().getBusinessPartnerId()))
        );
        var partnerAfter = businessPartnerRepository.findById(BusinessEntityTestData.DEFAULT_ENTITY).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(partnerAfter.getVersion()).isEqualTo(partnerBefore.getVersion());
    }

    @Test
    @WithJeapAuthenticationToken(bpRoles = { BusinessEntityTestData.DEFAULT_ENTITY_S + " = ti_@businesspartner_#sync" })
    void syncSingle_isIdempotentAndCanBeCalledRepeatedly() throws Exception {
        mockMvc
            .perform(post(BASE_URL + "/sync/" + BusinessEntityTestData.DEFAULT_ENTITY_S))
            .andExpect(status().isNoContent());
        mockMvc
            .perform(post(BASE_URL + "/sync/" + BusinessEntityTestData.DEFAULT_ENTITY_S))
            .andExpect(status().isNoContent());

        verify(domainEventPublisher, times(2)).publishTiBusinessPartnerUpdatedEvent(
            argThat(event -> BusinessEntityTestData.DEFAULT_ENTITY.equals(event.getPayload().getBusinessPartnerId()))
        );
    }

    @Test
    @WithJeapAuthenticationToken(userRoles = "ti_@businesspartner_#sync")
    // wildcard, like the devops profile
    void syncSingle_unknownPartner_returnsNotFound() throws Exception {
        mockMvc.perform(post(BASE_URL + "/sync/" + UUID.randomUUID())).andExpect(status().isNotFound());

        verifyNoInteractions(domainEventPublisher);
    }

    @Test
    @WithJeapAuthenticationToken(bpRoles = { BusinessEntityTestData.DEFAULT_ENTITY_S + " = ti_@status_#write" })
    void syncSingle_withoutTheWriteRole_isForbidden() throws Exception {
        mockMvc
            .perform(post(BASE_URL + "/sync/" + BusinessEntityTestData.DEFAULT_ENTITY_S))
            .andExpect(status().isForbidden());

        verifyNoInteractions(domainEventPublisher);
    }

    @Test
    @WithJeapAuthenticationToken(userRoles = "ti_@businesspartner_#sync")
    // global role, like the devops profile
    void syncAll_publishesOneEventPerPartner() throws Exception {
        var partnerCount = businessPartnerRepository.count();

        mockMvc.perform(post(BASE_URL + "/sync")).andExpect(status().isNoContent());

        verify(domainEventPublisher, times((int) partnerCount)).publishTiBusinessPartnerUpdatedEvent(
            org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    @WithJeapAuthenticationToken(bpRoles = { BusinessEntityTestData.DEFAULT_ENTITY_S + " = ti_@status_#write" })
    void syncAll_withoutTheWriteRole_isForbidden() throws Exception {
        mockMvc.perform(post(BASE_URL + "/sync")).andExpect(status().isForbidden());

        verifyNoInteractions(domainEventPublisher);
    }

    /**
     * Sync-all is an admin endpoint: a write role for a single partner must not be enough.
     */
    @Test
    @WithJeapAuthenticationToken(bpRoles = { BusinessEntityTestData.DEFAULT_ENTITY_S + " = ti_@businesspartner_#sync" })
    void syncAll_withOnlyASinglePartnerRole_isForbidden() throws Exception {
        // the same partner-bound role is sufficient for the partner-scoped endpoint ...
        mockMvc
            .perform(post(BASE_URL + "/sync/" + BusinessEntityTestData.DEFAULT_ENTITY_S))
            .andExpect(status().isNoContent());

        // ... but not for the admin endpoint spanning all partners
        mockMvc.perform(post(BASE_URL + "/sync")).andExpect(status().isForbidden());

        // only the partner-scoped sync published an event
        verify(domainEventPublisher, times(1)).publishTiBusinessPartnerUpdatedEvent(
            argThat(event -> BusinessEntityTestData.DEFAULT_ENTITY.equals(event.getPayload().getBusinessPartnerId()))
        );
    }
}
