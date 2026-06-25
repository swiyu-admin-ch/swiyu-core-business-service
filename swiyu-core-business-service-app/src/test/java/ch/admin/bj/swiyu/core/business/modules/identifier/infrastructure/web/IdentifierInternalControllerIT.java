package ch.admin.bj.swiyu.core.business.modules.identifier.infrastructure.web;

import static ch.admin.bj.swiyu.core.business.test.IdentifierTestData.identifierEntry;
import static ch.admin.bj.swiyu.core.business.test.IdentifierTestData.identifierEntry_Initialized;
import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.bit.jeap.security.test.WithJeapAuthenticationToken;
import ch.admin.bj.swiyu.core.business.modules.identifier.api.IdentifierEntryDto;
import ch.admin.bj.swiyu.core.business.modules.identifier.domain.IdentifierEntryRepository;
import ch.admin.bj.swiyu.core.business.modules.identifier.service.IdentifierEntryService;
import ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData;
import ch.admin.bj.swiyu.core.business.test.RestResponsePage;
import ch.admin.bj.swiyu.core.business.test.container.WithAllTestContainerInitializers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * For JeapAuthanticationToken see the <a href="https://bitbucket.bit.admin.ch/projects/JEAP/repos/jeap-spring-boot-starters/browse/jeap-spring-boot-security-starter-test">the README examples</a>
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@WithAllTestContainerInitializers
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "/delete_business_entities.sql")
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "/insert_test_business_entities.sql")
class IdentifierInternalControllerIT {

    static final String BASE_URL = "/api/v1/internal/identifier";

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    IdentifierEntryService identifierEntryService;

    @Autowired
    IdentifierEntryRepository identifierEntryRepository;

    @Test
    @WithJeapAuthenticationToken(bpRoles = { BusinessEntityTestData.ENTITY_A_S + " = ti_@identifier_#read" })
    void authorizedGetIdentifierEntry() throws Exception {
        // GIVEN
        var entry1 = identifierEntryRepository.save(
            identifierEntry_Initialized(
                BusinessEntityTestData.ENTITY_A,
                "did:swiyu:2abc96db-2ade-4b6c-baaf-b4f461cdabed"
            )
        );
        var entry2 = identifierEntryRepository.save(
            identifierEntry_Initialized(
                BusinessEntityTestData.ENTITY_A,
                "did:swiyu:3abc96db-2ade-4b6c-baaf-b4f461cdabed"
            )
        );
        identifierEntryRepository.save(identifierEntry(BusinessEntityTestData.ENTITY_B));

        // WHEN
        var result = objectMapper.readValue(
            mockMvc
                .perform(
                    MockMvcRequestBuilders.get(
                        BASE_URL + "/business-entities/" + BusinessEntityTestData.ENTITY_A_S + "/identifier/"
                    )
                )
                .andReturn()
                .getResponse()
                .getContentAsString(),
            new TypeReference<RestResponsePage<IdentifierEntryDto>>() {}
        );
        // THEN
        assertThat(result).isNotNull();
        // 3. status list should not be visible as it belongs to another business entity
        assertThat(result.getMetadata().totalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);

        // inverted order of occurrence as we default sort by updatedAt
        var item01 = result.getContent().get(0);
        assertThat(item01.id()).isEqualTo(entry2.getId());
        var item02 = result.getContent().get(1);
        assertThat(item02.id()).isEqualTo(entry1.getId());
    }

    @Test
    @WithJeapAuthenticationToken(bpRoles = { BusinessEntityTestData.ENTITY_A_S + " = ti_@identifier_#read" })
    void unauthorizedGetIdentifierEntry() throws Exception {
        // GIVEN
        identifierEntryRepository.save(identifierEntry(BusinessEntityTestData.ENTITY_B));
        // WHEN
        var result = mockMvc
            .perform(
                MockMvcRequestBuilders.get(
                    BASE_URL + "/business-entities/" + BusinessEntityTestData.ENTITY_B_S + "/identifier/"
                )
            )
            .andReturn()
            .getResponse();
        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(403);
    }
}
