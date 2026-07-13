package ch.admin.bj.swiyu.core.business.modules.identifier.infrastructure.web;

import static ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.admin.bit.jeap.security.test.WithJeapAuthenticationToken;
import ch.admin.bj.swiyu.core.business.common.utils.testSpan.WithTestSpan;
import ch.admin.bj.swiyu.core.business.modules.identifier.api.IdentifierEntryDto;
import ch.admin.bj.swiyu.core.business.modules.identifier.config.IdentifierLimitProperties;
import ch.admin.bj.swiyu.core.business.modules.identifier.service.IdentifierEntryService;
import ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData;
import ch.admin.bj.swiyu.core.business.test.IdentifierTestData;
import ch.admin.bj.swiyu.core.business.test.RestResponsePage;
import ch.admin.bj.swiyu.core.business.test.container.WithAllTestContainerInitializers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.unit.DataSize;

/**
 * For JeapAuthanticationToken see the <a href="https://bitbucket.bit.admin.ch/projects/JEAP/repos/jeap-spring-boot-starters/browse/jeap-spring-boot-security-starter-test">the README examples</a>
 */
@ActiveProfiles("test")
@SpringBootTest
@EmbeddedKafka
@AutoConfigureMockMvc
@WithAllTestContainerInitializers
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "/delete_business_entities.sql")
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "/insert_test_business_entities.sql")
@TestMethodOrder(MethodOrderer.MethodName.class)
class IdentifierB2BControllerIT {

    static final String BASE_URL = "/api/v1/identifier/";

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    IdentifierEntryService identifierEntryService;

    @Autowired
    IdentifierTestData identifierTestData;

    @MockitoSpyBean
    IdentifierLimitProperties identifierLimitProperties;

    @Test
    @WithJeapAuthenticationToken(bpRoles = { DEFAULT_ENTITY_S + " = ti_@identifier_#write" })
    void createIdentifierEntry_authorized() throws Exception {
        // WHEN
        var result = objectMapper.readValue(
            mockMvc
                .perform(
                    MockMvcRequestBuilders.post(
                        BASE_URL + "business-entities/" + DEFAULT_ENTITY_S + "/identifier-entries/"
                    )
                )
                .andReturn()
                .getResponse()
                .getContentAsString(),
            IdentifierEntryDto.class
        );
        // THEN
        assertThat(result).isNotNull();
        assertThat(result.id()).isNotNull();
    }

    @Test
    @WithJeapAuthenticationToken(bpRoles = { BusinessEntityTestData.DEFAULT_ENTITY_S + " = ti_@businesspartner_#read" })
    void createIdentifierEntry_unauthorized() throws Exception {
        // WHEN
        var result = mockMvc
            .perform(
                MockMvcRequestBuilders.post(
                    BASE_URL + "business-entities/" + BusinessEntityTestData.DEFAULT_ENTITY_S + "/identifier-entries/"
                )
            )
            .andReturn()
            .getResponse();
        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(403);
    }

    @Test
    @WithJeapAuthenticationToken(bpRoles = { BusinessEntityTestData.ENTITY_A_S + " = ti_@identifier_#read" })
    void createIdentifierEntry_wrongAuthorized() throws Exception {
        // WHEN
        var result = mockMvc
            .perform(
                MockMvcRequestBuilders.post(
                    BASE_URL + "business-entities/" + BusinessEntityTestData.ENTITY_B_S + "/identifier-entries/"
                )
            )
            .andReturn()
            .getResponse();
        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(403);
    }

    @Test
    @WithJeapAuthenticationToken(bpRoles = { BusinessEntityTestData.DEFAULT_ENTITY_S + " = ti_@identifier_#write" })
    @WithTestSpan
    void updateIdentifierEntry_authorized() throws Exception {
        // GIVEN
        // BusinessEntity provided through SQL
        var identifierEntryId = identifierEntryService.createIdentifierEntry(DEFAULT_ENTITY).id();
        // WHEN
        var result = mockMvc
            .perform(
                MockMvcRequestBuilders.put(
                    BASE_URL +
                        "business-entities/" +
                        BusinessEntityTestData.DEFAULT_ENTITY_S +
                        "/identifier-entries/" +
                        identifierEntryId
                )
                    .content(identifierTestData.validDidTdwLog())
                    .contentType("application/jsonl+json")
            )
            .andReturn()
            .getResponse();
        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(200);
    }

    @Test
    @WithJeapAuthenticationToken(bpRoles = { BusinessEntityTestData.DEFAULT_ENTITY_S + " = ti_@identifier_#write" })
    @WithTestSpan
    void updateIdentifierEntry_authorizedWithDidV1_2_0() throws Exception {
        // GIVEN
        // BusinessEntity provided through SQL
        var identifierEntryId = identifierEntryService.createIdentifierEntry(DEFAULT_ENTITY).id();
        // WHEN
        var result = mockMvc
            .perform(
                MockMvcRequestBuilders.put(
                    BASE_URL +
                        "business-entities/" +
                        BusinessEntityTestData.DEFAULT_ENTITY_S +
                        "/identifier-entries/" +
                        identifierEntryId
                )
                    .content(identifierTestData.validDidTdwLogV1_2_0())
                    .contentType("application/jsonl+json")
            )
            .andReturn()
            .getResponse();
        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(200);
    }

    @Test
    @WithJeapAuthenticationToken(bpRoles = { BusinessEntityTestData.DEFAULT_ENTITY_S + " = ti_@identifier_#write" })
    @WithTestSpan
    void updateIdentifierEntry_authorizedWithDidV2_0_0() throws Exception {
        // GIVEN
        // BusinessEntity provided through SQL
        var identifierEntryId = identifierEntryService.createIdentifierEntry(DEFAULT_ENTITY).id();
        // WHEN
        var result = mockMvc
            .perform(
                MockMvcRequestBuilders.put(
                    BASE_URL +
                        "business-entities/" +
                        BusinessEntityTestData.DEFAULT_ENTITY_S +
                        "/identifier-entries/" +
                        identifierEntryId
                )
                    .content(identifierTestData.validDidWebvhLogV2_0_0())
                    .contentType("application/jsonl+json")
            )
            .andReturn()
            .getResponse();
        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(200);
    }

    @Test
    @WithJeapAuthenticationToken(bpRoles = { BusinessEntityTestData.DEFAULT_ENTITY_S + " = ti_@identifier_#write" })
    @WithTestSpan
    void updateIdentifierEntry_authorizedWithDidV2_1_0() throws Exception {
        // GIVEN
        // BusinessEntity provided through SQL
        var identifierEntryId = identifierEntryService.createIdentifierEntry(DEFAULT_ENTITY).id();
        // WHEN
        var result = mockMvc
            .perform(
                MockMvcRequestBuilders.put(
                    BASE_URL +
                        "business-entities/" +
                        BusinessEntityTestData.DEFAULT_ENTITY_S +
                        "/identifier-entries/" +
                        identifierEntryId
                )
                    .content(identifierTestData.validDidWebvhLogV2_1_0())
                    .contentType("application/jsonl+json")
            )
            .andReturn()
            .getResponse();
        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(200);
    }

    @Test
    @WithJeapAuthenticationToken(bpRoles = { BusinessEntityTestData.DEFAULT_ENTITY_S + " = ti_@identifier_#write" })
    @WithTestSpan
    void updateIdentifierEntry_authoirzedWithValidDidWebvhEntry() throws Exception {
        // GIVEN
        // BusinessEntity provided through SQL
        var identifierEntryId = identifierEntryService.createIdentifierEntry(DEFAULT_ENTITY).id();
        // WHEN
        var result = mockMvc
            .perform(
                MockMvcRequestBuilders.put(
                    BASE_URL +
                        "business-entities/" +
                        BusinessEntityTestData.DEFAULT_ENTITY_S +
                        "/identifier-entries/" +
                        identifierEntryId
                )
                    .content(identifierTestData.validDidWebvhEntry())
                    .contentType("application/jsonl+json")
            )
            .andReturn()
            .getResponse();
        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(200);
    }

    @Test
    @WithJeapAuthenticationToken(bpRoles = { BusinessEntityTestData.DEFAULT_ENTITY_S + " = ti_@identifier_#write" })
    @WithTestSpan
    void updateIdentifierEntry_authorizedWithValidDidWebvhUpdatedEntry() throws Exception {
        // GIVEN
        // BusinessEntity provided through SQL
        var identifierEntryId = identifierEntryService.createIdentifierEntry(DEFAULT_ENTITY).id();
        // WHEN
        var result = mockMvc
            .perform(
                MockMvcRequestBuilders.put(
                    BASE_URL +
                        "business-entities/" +
                        BusinessEntityTestData.DEFAULT_ENTITY_S +
                        "/identifier-entries/" +
                        identifierEntryId
                )
                    .content(identifierTestData.validDidWebvhUpdatedEntry())
                    .contentType("application/jsonl+json")
            )
            .andReturn()
            .getResponse();
        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(200);
    }

    @Test
    @WithJeapAuthenticationToken(bpRoles = { BusinessEntityTestData.DEFAULT_ENTITY_S + " = ti_@identifier_#write" })
    @WithTestSpan
    void updateIdentifierEntry_invalidJson() throws Exception {
        // GIVEN
        // BusinessEntity provided through SQL
        var identifierEntryId = identifierEntryService.createIdentifierEntry(DEFAULT_ENTITY).id();
        // WHEN
        mockMvc
            .perform(
                MockMvcRequestBuilders.put(
                    BASE_URL +
                        "business-entities/" +
                        BusinessEntityTestData.DEFAULT_ENTITY_S +
                        "/identifier-entries/" +
                        identifierEntryId
                )
                    .content(identifierTestData.invalidJsonl())
                    .contentType("application/jsonl+json")
            )
            // THEN
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("identifier_validation_failed"))
            .andExpect(jsonPath("$.message").value("Provided identifier resource is invalid."))
            .andExpect(jsonPath("$.additionalDetails").isArray())
            .andExpect(jsonPath("$.additionalDetails.length()").value(1))
            .andExpect(
                jsonPath("$.additionalDetails.[0]").value(
                    """
                    Unexpected character ('A' (code 65)): was expecting comma to separate Array entries
                     at [Source: (String)"["7xdZfUFPCp3LJ84Ca62WSEWNjuNWVtJRikW7HWNLuNisreJdGNdJoEzNHkXKg4fWSMnqsPv3YXpo7AZhJ2Ej4taqbUrwh",2,"2024-12-18T15:51:54.828+0000",{},{"value":{"@context":["https://www.w3.org/ns/did/v1","https://w3id.org/security/multikey/v1"],"id":"did:tdw:8FNcNQJYDYBHUveSaDetnbAWkDvhDeei8b2RqcLc8jbFcD1tZ8FoSdA5uHQQY9c1G7nQAeW3TBxkTCfce5KR6ePgkxeUU:identifier-reg-r.trust-infra.swiyu.admin.ch:api:v1:did:b361d375-0ac8-42e5-b553-65ac6e2c4b8b","verificationMethod":[{"id":"did:tdw:8FNcNQJYDYBHUveSaDetnbAWkDvhDeei8b2"[truncated 2631 chars]; line: 1, column: 3130]"""
                )
            );
    }

    @Test
    @WithJeapAuthenticationToken(bpRoles = { BusinessEntityTestData.DEFAULT_ENTITY_S + " = ti_@identifier_#write" })
    @WithTestSpan
    void updateIdentifierEntry_unknownDidMethod() throws Exception {
        // GIVEN
        // BusinessEntity provided through SQL
        var identifierEntryId = identifierEntryService.createIdentifierEntry(DEFAULT_ENTITY).id();
        // WHEN
        mockMvc
            .perform(
                MockMvcRequestBuilders.put(
                    BASE_URL +
                        "business-entities/" +
                        BusinessEntityTestData.DEFAULT_ENTITY_S +
                        "/identifier-entries/" +
                        identifierEntryId
                )
                    .content("just a random string that is not a did log at all")
                    .contentType("application/jsonl+json")
            )
            // THEN
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("identifier_validation_failed"))
            .andExpect(jsonPath("$.message").value("Provided identifier resource is invalid."))
            .andExpect(jsonPath("$.additionalDetails").isArray())
            .andExpect(jsonPath("$.additionalDetails.length()").value(1))
            .andExpect(
                jsonPath("$.additionalDetails.[0]").value(
                    "Unknown DID method in log entry: neither 'did:tdw:' nor 'did:webvh:' found"
                )
            );
    }

    @Test
    @WithJeapAuthenticationToken(bpRoles = { BusinessEntityTestData.DEFAULT_ENTITY_S + " = ti_@identifier_#write" })
    @WithTestSpan
    void updateIdentifierEntry_invalidDidWebvhWrongBaseRegister() throws Exception {
        // GIVEN
        // BusinessEntity provided through SQL
        var identifierEntryId = identifierEntryService.createIdentifierEntry(DEFAULT_ENTITY).id();
        // WHEN
        mockMvc
            .perform(
                MockMvcRequestBuilders.put(
                    BASE_URL +
                        "business-entities/" +
                        BusinessEntityTestData.DEFAULT_ENTITY_S +
                        "/identifier-entries/" +
                        identifierEntryId
                )
                    .content(identifierTestData.invalidDidWebvhWrongBaseRegister())
                    .contentType("application/jsonl+json")
            )
            // THEN
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("identifier_validation_failed"))
            .andExpect(jsonPath("$.message").value("Provided identifier resource is invalid."))
            .andExpect(jsonPath("$.additionalDetails").isArray())
            .andExpect(jsonPath("$.additionalDetails.length()").value(1))
            .andExpect(
                jsonPath("$.additionalDetails.[0]").value(
                    "DID points to an unknown base registry. Your data: 'https://example.com/.well-known/did.jsonl' Expected prefix: 'https://identifier-reg-d.trust-infra.swiyu.admin.ch/api/v1/did/'"
                )
            );
    }

    @Test
    @WithJeapAuthenticationToken(bpRoles = { BusinessEntityTestData.DEFAULT_ENTITY_S + " = ti_@identifier_#write" })
    @WithTestSpan
    void updateIdentifierEntry_invalidDataIntegrity() throws Exception {
        // GIVEN
        // BusinessEntity provided through SQL
        var identifierEntryId = identifierEntryService.createIdentifierEntry(DEFAULT_ENTITY).id();
        // WHEN
        mockMvc
            .perform(
                MockMvcRequestBuilders.put(
                    BASE_URL +
                        "business-entities/" +
                        BusinessEntityTestData.DEFAULT_ENTITY_S +
                        "/identifier-entries/" +
                        identifierEntryId
                )
                    .content(identifierTestData.invalidDidTdwLogDataIntegrity())
                    .contentType("application/jsonl+json")
            )
            // THEN
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("identifier_validation_failed"))
            .andExpect(jsonPath("$.message").value("Provided identifier resource is invalid."))
            .andExpect(jsonPath("$.additionalDetails").isArray())
            .andExpect(jsonPath("$.additionalDetails.length()").value(1))
            .andExpect(
                jsonPath("$.additionalDetails.[0]").value(
                    "invalid DID log integration proof: Failed to verify proof due to: failure of a signature to satisfy the verification equation: signature error: Verification equation was not satisfied"
                )
            );
    }

    @Test
    @WithJeapAuthenticationToken(bpRoles = { BusinessEntityTestData.DEFAULT_ENTITY_S + " = ti_@identifier_#write" })
    @WithTestSpan
    void updateIdentifierEntry_invalidPortable() throws Exception {
        // GIVEN
        // BusinessEntity provided through SQL
        var identifierEntryId = identifierEntryService.createIdentifierEntry(DEFAULT_ENTITY).id();
        // WHEN

        mockMvc
            .perform(
                MockMvcRequestBuilders.put(
                    BASE_URL +
                        "business-entities/" +
                        BusinessEntityTestData.DEFAULT_ENTITY_S +
                        "/identifier-entries/" +
                        identifierEntryId
                )
                    .content(identifierTestData.invalidDidLogPortable())
                    .contentType("application/jsonl+json")
            )
            // THEN
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("identifier_validation_failed"))
            .andExpect(jsonPath("$.message").value("Provided identifier resource is invalid."))
            .andExpect(jsonPath("$.additionalDetails").isArray())
            .andExpect(jsonPath("$.additionalDetails.length()").value(1))
            .andExpect(
                jsonPath("$.additionalDetails.[0]").value(
                    """
                    [/2/portable: must be the constant value 'false']"""
                )
            );
    }

    @Test
    @WithJeapAuthenticationToken(
        bpRoles = { BusinessEntityTestData.DEFAULT_ENTITY_S + " = ti_@businesspartner_#write" }
    )
    @WithTestSpan
    void unauthorizedUpdateIdentifierEntry() throws Exception {
        // GIVEN
        // BusinessEntity provided through SQL
        var identifierEntryId = identifierEntryService.createIdentifierEntry(DEFAULT_ENTITY).id();
        // WHEN
        var result = mockMvc
            .perform(
                MockMvcRequestBuilders.put(
                    BASE_URL +
                        "business-entities/" +
                        BusinessEntityTestData.DEFAULT_ENTITY_S +
                        "/identifier-entries/" +
                        identifierEntryId
                )
                    .content(identifierTestData.validDidTdwLog())
                    .contentType("application/jsonl+json")
            )
            .andReturn()
            .getResponse();
        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(403);
    }

    @Test
    @WithJeapAuthenticationToken(bpRoles = { ENTITY_A_S + " = ti_@identifier_#write" })
    @WithTestSpan
    void updateIdentifierEntry_wrongAuthorized() throws Exception {
        // GIVEN
        // BusinessEntity provided through SQL
        var identifierEntryId = identifierEntryService.createIdentifierEntry(ENTITY_B).id();
        // WHEN
        var result = mockMvc
            .perform(
                MockMvcRequestBuilders.put(
                    BASE_URL + "business-entities/" + ENTITY_A_S + "/identifier-entries/" + identifierEntryId
                )
                    .content(identifierTestData.validDidTdwLog())
                    .contentType("application/jsonl+json")
            )
            .andReturn()
            .getResponse();
        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(404);
    }

    @Test
    @WithJeapAuthenticationToken(bpRoles = { BusinessEntityTestData.ENTITY_A_S + " = ti_@identifier_#read" })
    @WithTestSpan
    void getAllIdentifierEntries_authorized() throws Exception {
        // GIVEN
        // BusinessEntity provided through SQL
        var identifierEntryId01 = identifierEntryService.createIdentifierEntry(ENTITY_A).id();
        var identifierEntryId02 = identifierEntryService.createIdentifierEntry(ENTITY_A).id();
        identifierEntryService.createIdentifierEntry(ENTITY_B).id();
        // WHEN
        var result = objectMapper.readValue(
            mockMvc
                .perform(
                    MockMvcRequestBuilders.get(
                        BASE_URL + "business-entities/" + BusinessEntityTestData.ENTITY_A_S + "/identifier/"
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
        assertThat(item01.id()).isEqualTo(identifierEntryId02);
        var item02 = result.getContent().get(1);
        assertThat(item02.id()).isEqualTo(identifierEntryId01);
    }

    @Test
    @WithJeapAuthenticationToken(bpRoles = { BusinessEntityTestData.ENTITY_A_S + " = ti_@businesspartner_#read" })
    @WithTestSpan
    void unauthorizedGetIdentifierEntry() throws Exception {
        // GIVEN
        // BusinessEntity provided through SQL
        identifierEntryService.createIdentifierEntry(ENTITY_B).id();
        // WHEN
        var result = mockMvc
            .perform(
                MockMvcRequestBuilders.get(
                    BASE_URL + "business-entities/" + BusinessEntityTestData.ENTITY_B_S + "/identifier/"
                )
            )
            .andReturn()
            .getResponse();
        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(403);
    }

    @Test
    @WithJeapAuthenticationToken(bpRoles = { BusinessEntityTestData.DEFAULT_ENTITY_S + " = ti_@identifier_#write" })
    @WithTestSpan
    void updateIdentifierEntry_SizeLimitDidLog() throws Exception {
        // GIVEN
        doReturn(new IdentifierLimitProperties.DidLogLimits(DataSize.ofBytes(1L)))
            .when(identifierLimitProperties)
            .didLog();
        var identifierEntryId = identifierEntryService.createIdentifierEntry(DEFAULT_ENTITY).id();

        // WHEN
        mockMvc
            .perform(
                MockMvcRequestBuilders.put(
                    BASE_URL +
                        "business-entities/" +
                        BusinessEntityTestData.DEFAULT_ENTITY_S +
                        "/identifier-entries/" +
                        identifierEntryId
                )
                    .content(identifierTestData.validDidTdwLog())
                    .contentType("application/jsonl+json")
            )
            // THEN
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("max_size_exceeded"))
            .andExpect(jsonPath("$.message").value("Uploaded data exceeds the maximum size."))
            .andExpect(jsonPath("$.additionalDetails[0]").value(StringContains.containsString("DidLog")));
    }

    @Test
    @WithJeapAuthenticationToken(bpRoles = { DEFAULT_ENTITY_S + " = ti_@identifier_#write" })
    @WithTestSpan
    void updateIdentifierEntry_SizeLimitDidDoc() throws Exception {
        // GIVEN
        // BusinessEntity provided through SQL
        doReturn(new IdentifierLimitProperties.DidDocLimits(DataSize.ofBytes(1L)))
            .when(identifierLimitProperties)
            .didDoc();
        var identifierEntryId = identifierEntryService.createIdentifierEntry(DEFAULT_ENTITY).id();
        // WHEN
        mockMvc
            .perform(
                MockMvcRequestBuilders.put(
                    BASE_URL + "business-entities/" + DEFAULT_ENTITY_S + "/identifier-entries/" + identifierEntryId
                )
                    .content(identifierTestData.validDidTdwLog())
                    .contentType("application/jsonl+json")
            )
            // THEN
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("max_size_exceeded"))
            .andExpect(jsonPath("$.message").value("Uploaded data exceeds the maximum size."))
            .andExpect(jsonPath("$.additionalDetails[0]").value(StringContains.containsString("DidDoc")));
    }
}
