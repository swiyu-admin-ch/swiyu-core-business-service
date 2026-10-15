package ch.admin.bj.swiyu.core.business.modules.payment.infrastructure.web;

import static ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData.ENTITY_A;
import static ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData.ENTITY_A_S;
import static ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData.ENTITY_B_S;
import static ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData.insertTestBusinessPartners;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.admin.bit.jeap.security.test.WithJeapAuthenticationToken;
import ch.admin.bj.swiyu.core.business.common.api.ProductTypeDto;
import ch.admin.bj.swiyu.core.business.common.features.FeaturesProperties;
import ch.admin.bj.swiyu.core.business.modules.payment.api.PaymentInitializationRequestDto;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.SaferpayClient;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.SaferpayInitializeResult;
import ch.admin.bj.swiyu.core.business.test.TestRepositories;
import ch.admin.bj.swiyu.core.business.test.WithExtendedJeapAuthenticationToken;
import ch.admin.bj.swiyu.core.business.test.container.WithAllTestContainerInitializers;
import java.net.MalformedURLException;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.ObjectMapper;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@WithAllTestContainerInitializers
class PaymentInternalControllerIT {

    private static final String INITIATE_URL = "/api/v2/internal/payment/initiate";
    private static final String WRITE_ROLE = "ti_@payment_#write";

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    TestRepositories repos;

    @MockitoBean
    SaferpayClient saferpayClient;

    @MockitoBean
    FeaturesProperties featuresProperties;

    @BeforeEach
    void setUp() {
        insertTestBusinessPartners(repos.businessPartner);
        repos.payment.deleteAll();
        when(featuresProperties.getEidartfe1297Payment()).thenReturn(true);
        when(saferpayClient.initialize(any())).thenReturn(
            new SaferpayInitializeResult(
                "mock-token",
                "https://saferpay.example/redirect/abc",
                Instant.now().plusSeconds(600)
            )
        );
    }

    @Test
    @WithJeapAuthenticationToken(userRoles = WRITE_ROLE)
    void initiatePayment_authorized_thenCreatedAndPersisted() throws Exception {
        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.post(INITIATE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(request(ENTITY_A, "https://portal.example.local/callback"))
                    )
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.businessPartnerId").value(ENTITY_A.toString()))
            .andExpect(jsonPath("$.redirectUrl").value("https://saferpay.example/redirect/abc"))
            .andReturn();

        var paymentId = UUID.fromString(
            objectMapper.readTree(response.getResponse().getContentAsString()).get("paymentId").asString()
        );
        var payment = repos.payment.findById(paymentId).orElseThrow();
        assertThat(payment.getReferenceId()).matches("^P-[0-9A-F]{8}$");
        assertThat(payment.getSaferpayToken()).isEqualTo("mock-token");
        assertThat(payment.getSaferpayRedirectUrl()).isEqualTo("https://saferpay.example/redirect/abc");
        assertThat(payment.getSaferpayExpiration()).isNotNull();
    }

    @Test
    @WithExtendedJeapAuthenticationToken(bpRoles = ENTITY_A_S + "=" + WRITE_ROLE)
    void initiatePayment_roleForOwnPartner_thenCreated() throws Exception {
        mockMvc
            .perform(
                MockMvcRequestBuilders.post(INITIATE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(request(ENTITY_A, "https://portal.example.local/callback"))
                    )
            )
            .andExpect(status().isCreated());
    }

    @Test
    @WithExtendedJeapAuthenticationToken(bpRoles = ENTITY_B_S + "=" + WRITE_ROLE)
    void initiatePayment_roleForOtherPartner_thenForbidden() throws Exception {
        mockMvc
            .perform(
                MockMvcRequestBuilders.post(INITIATE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(request(ENTITY_A, "https://portal.example.local/callback"))
                    )
            )
            .andExpect(status().isForbidden());

        assertThat(repos.payment.findAll()).isEmpty();
    }

    @Test
    @WithJeapAuthenticationToken
    void initiatePayment_missingRole_thenForbidden() throws Exception {
        mockMvc
            .perform(
                MockMvcRequestBuilders.post(INITIATE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(request(ENTITY_A, "https://portal.example.local/callback"))
                    )
            )
            .andExpect(status().isForbidden());
    }

    @Test
    @WithJeapAuthenticationToken(userRoles = WRITE_ROLE)
    void initiatePayment_returnUrlNotAllowed_thenBadRequest() throws Exception {
        mockMvc
            .perform(
                MockMvcRequestBuilders.post(INITIATE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request(ENTITY_A, "https://evil.example.com/callback")))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("payment_return_url_not_allowed"));
    }

    @Test
    @WithJeapAuthenticationToken(userRoles = WRITE_ROLE)
    void initiatePayment_featureDisabled_thenBadRequest() throws Exception {
        when(featuresProperties.getEidartfe1297Payment()).thenReturn(false);

        mockMvc
            .perform(
                MockMvcRequestBuilders.post(INITIATE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(request(ENTITY_A, "https://portal.example.local/callback"))
                    )
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("payment_disabled"));
    }

    @Test
    @WithJeapAuthenticationToken(userRoles = WRITE_ROLE)
    void initiatePayment_pendingPaymentAlreadyExists_thenBadRequest() throws Exception {
        var content = objectMapper.writeValueAsString(request(ENTITY_A, "https://portal.example.local/callback"));
        mockMvc
            .perform(MockMvcRequestBuilders.post(INITIATE_URL).contentType(MediaType.APPLICATION_JSON).content(content))
            .andExpect(status().isCreated());

        mockMvc
            .perform(MockMvcRequestBuilders.post(INITIATE_URL).contentType(MediaType.APPLICATION_JSON).content(content))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("payment_already_pending"));

        assertThat(repos.payment.findAll()).hasSize(1);
    }

    private PaymentInitializationRequestDto request(UUID businessPartnerId, String returnUrl)
        throws MalformedURLException {
        return new PaymentInitializationRequestDto(
            businessPartnerId,
            ProductTypeDto.BASE_REGISTRATION,
            URI.create(returnUrl).toURL()
        );
    }
}
