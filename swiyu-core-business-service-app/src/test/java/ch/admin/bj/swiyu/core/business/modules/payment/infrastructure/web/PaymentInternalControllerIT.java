package ch.admin.bj.swiyu.core.business.modules.payment.infrastructure.web;

import static ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData.ENTITY_A;
import static ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData.ENTITY_A_S;
import static ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData.ENTITY_B;
import static ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData.ENTITY_B_S;
import static ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData.insertTestBusinessPartners;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.admin.bit.jeap.security.test.WithJeapAuthenticationToken;
import ch.admin.bj.swiyu.core.business.common.api.ProductTypeDto;
import ch.admin.bj.swiyu.core.business.modules.payment.api.PaymentDto;
import ch.admin.bj.swiyu.core.business.modules.payment.api.PaymentInitializationRequestDto;
import ch.admin.bj.swiyu.core.business.modules.payment.api.PaymentRetryRequestDto;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.Amount;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.Payment;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.PaymentStatus;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.ProductType;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.SaferpayClient;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.SaferpayInitializePaymentCommand;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.SaferpayInitializeResult;
import ch.admin.bj.swiyu.core.business.test.RestResponsePage;
import ch.admin.bj.swiyu.core.business.test.TestRepositories;
import ch.admin.bj.swiyu.core.business.test.WithExtendedJeapAuthenticationToken;
import ch.admin.bj.swiyu.core.business.test.container.WithAllTestContainerInitializers;
import java.net.MalformedURLException;
import java.net.URI;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@WithAllTestContainerInitializers
class PaymentInternalControllerIT {

    private static final String INITIATE_URL = "/api/v2/internal/payment/initiate";
    private static final String PAYMENT_URL = "/api/v2/internal/payment";
    private static final String RETRY_URL = "/api/v2/internal/payment/retry";
    private static final String WRITE_ROLE = "ti_@payment_#write";
    private static final String READ_ROLE = "ti_@payment_#read";

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    TestRepositories repos;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @MockitoBean
    SaferpayClient saferpayClient;

    @BeforeEach
    void setUp() {
        repos.truncateTables();
        insertTestBusinessPartners(repos.businessPartner);
        repos.payment.deleteAll();
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

    @Test
    @WithJeapAuthenticationToken(userRoles = READ_ROLE)
    void getPayment_authorized_thenOk() throws Exception {
        var payment = savePayment(ENTITY_A, "P-00000001");

        mockMvc
            .perform(MockMvcRequestBuilders.get(PAYMENT_URL + "/" + payment.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(payment.getId().toString()))
            .andExpect(jsonPath("$.businessPartnerId").value(ENTITY_A.toString()))
            .andExpect(jsonPath("$.referenceId").value("P-00000001"))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.product").value("BASE_REGISTRATION"))
            .andExpect(jsonPath("$.amount.value").value(15_000))
            .andExpect(jsonPath("$.amount.currency").value("CHF"))
            .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @WithExtendedJeapAuthenticationToken(bpRoles = ENTITY_B_S + "=" + READ_ROLE)
    void getPayment_roleForOtherPartner_thenForbidden() throws Exception {
        var payment = savePayment(ENTITY_A, "P-00000001");

        mockMvc
            .perform(MockMvcRequestBuilders.get(PAYMENT_URL + "/" + payment.getId()))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithJeapAuthenticationToken(userRoles = READ_ROLE)
    void getPayment_unknownId_thenNotFound() throws Exception {
        mockMvc
            .perform(MockMvcRequestBuilders.get(PAYMENT_URL + "/" + UUID.randomUUID()))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithJeapAuthenticationToken(userRoles = WRITE_ROLE)
    void getPayment_missingReadRole_thenForbidden() throws Exception {
        var payment = savePayment(ENTITY_A, "P-00000001");

        mockMvc
            .perform(MockMvcRequestBuilders.get(PAYMENT_URL + "/" + payment.getId()))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithJeapAuthenticationToken(userRoles = READ_ROLE)
    void getPayments_allPartners_thenNewestFirst() throws Exception {
        var older = savePayment(ENTITY_A, "P-00000001");
        var newer = savePayment(ENTITY_B, "P-00000002");
        jdbcTemplate.update(
            "UPDATE payment SET created_at = ? WHERE id = ?",
            Timestamp.from(Instant.now().minus(1, ChronoUnit.DAYS)),
            older.getId()
        );

        var page = getPayments(PAYMENT_URL);

        assertThat(page.getContent()).extracting(PaymentDto::id).containsExactly(newer.getId(), older.getId());
    }

    @Test
    @WithExtendedJeapAuthenticationToken(bpRoles = ENTITY_A_S + "=" + READ_ROLE)
    void getPayments_roleForOnePartner_thenOnlyOwnPayments() throws Exception {
        var own = savePayment(ENTITY_A, "P-00000001");
        savePayment(ENTITY_B, "P-00000002");

        var page = getPayments(PAYMENT_URL);

        assertThat(page.getContent()).extracting(PaymentDto::id).containsExactly(own.getId());
    }

    @Test
    @WithExtendedJeapAuthenticationToken(bpRoles = ENTITY_A_S + "=" + READ_ROLE)
    void getPayments_filterForOtherPartner_thenForbidden() throws Exception {
        mockMvc
            .perform(MockMvcRequestBuilders.get(PAYMENT_URL).param("businessPartnerIds", ENTITY_B_S))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithJeapAuthenticationToken(userRoles = READ_ROLE)
    void getPayments_filterByPartner_thenOnlyMatching() throws Exception {
        var own = savePayment(ENTITY_A, "P-00000001");
        savePayment(ENTITY_B, "P-00000002");

        var page = getPayments(PAYMENT_URL + "?businessPartnerIds=" + ENTITY_A_S);

        assertThat(page.getContent()).extracting(PaymentDto::id).containsExactly(own.getId());
    }

    @Test
    @WithJeapAuthenticationToken(userRoles = READ_ROLE)
    void getPayments_filterByStatus_thenOnlyMatching() throws Exception {
        var failed = savePayment(ENTITY_A, "P-00000001");
        failed.markAsFailed();
        repos.payment.saveAndFlush(failed);
        savePayment(ENTITY_A, "P-00000002");

        var page = getPayments(PAYMENT_URL + "?statuses=FAILED");

        assertThat(page.getContent()).extracting(PaymentDto::id).containsExactly(failed.getId());
    }

    @Test
    @WithJeapAuthenticationToken(userRoles = READ_ROLE)
    void getPayments_filterByProduct_thenOnlyMatching() throws Exception {
        savePayment(ENTITY_A, "P-00000001");
        savePayment(ENTITY_B, "P-00000002");

        var page = getPayments(PAYMENT_URL + "?productTypes=BASE_REGISTRATION");

        assertThat(page.getContent()).hasSize(2);
    }

    @Test
    @WithJeapAuthenticationToken(userRoles = READ_ROLE)
    void getPayments_paged_thenPageMetadata() throws Exception {
        savePayment(ENTITY_A, "P-00000001");
        savePayment(ENTITY_B, "P-00000002");

        var page = getPayments(PAYMENT_URL + "?page=0&size=1&sort=referenceId,asc");

        assertThat(page.getContent()).extracting(PaymentDto::referenceId).containsExactly("P-00000001");
        assertThat(page.getMetadata().totalElements()).isEqualTo(2);
    }

    @Test
    @WithJeapAuthenticationToken(userRoles = READ_ROLE)
    void getPayments_unsupportedSortField_thenBadRequest() throws Exception {
        mockMvc
            .perform(MockMvcRequestBuilders.get(PAYMENT_URL).param("sort", "saferpayToken"))
            .andExpect(status().isBadRequest());
    }

    private RestResponsePage<PaymentDto> getPayments(String url) throws Exception {
        return objectMapper.readValue(
            mockMvc
                .perform(MockMvcRequestBuilders.get(url))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            new TypeReference<RestResponsePage<PaymentDto>>() {}
        );
    }

    private Payment savePayment(UUID businessPartnerId, String referenceId) {
        var id = UUID.randomUUID();
        return repos.payment.saveAndFlush(
            new Payment(
                id,
                businessPartnerId,
                referenceId,
                ProductType.BASE_REGISTRATION,
                Amount.chf(15_000),
                "https://portal.example.local/callback/" + id
            )
        );
    }

    @Test
    @WithJeapAuthenticationToken(userRoles = WRITE_ROLE)
    void retryPayment_failedPayment_thenCreatedAndNewPaymentStored() throws Exception {
        var previous = failedPayment(ENTITY_A, "P-00000001");

        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.post(RETRY_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(retryRequest(previous.getId(), ENTITY_A)))
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.businessPartnerId").value(ENTITY_A.toString()))
            .andExpect(jsonPath("$.redirectUrl").value("https://saferpay.example/redirect/abc"))
            .andReturn();

        var newPaymentId = UUID.fromString(
            objectMapper.readTree(response.getResponse().getContentAsString()).get("paymentId").asString()
        );
        assertThat(newPaymentId).isNotEqualTo(previous.getId());
        var created = repos.payment.findById(newPaymentId).orElseThrow();
        assertThat(created.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(created.getBusinessPartnerId()).isEqualTo(ENTITY_A);
        assertThat(created.getProduct()).isEqualTo(ProductType.BASE_REGISTRATION);
        assertThat(created.getAmount().getValue()).isEqualTo(previous.getAmount().getValue());
        assertThat(created.getReferenceId()).matches("^P-[0-9A-F]{8}$").isNotEqualTo(previous.getReferenceId());
        assertThat(created.getReturnUrl()).isEqualTo("https://portal.example.local/callback/" + newPaymentId);
        assertThat(created.getSaferpayToken()).isEqualTo("mock-token");
        assertThat(repos.payment.findAll()).hasSize(2);

        var commandCaptor = ArgumentCaptor.forClass(SaferpayInitializePaymentCommand.class);
        verify(saferpayClient).initialize(commandCaptor.capture());
        assertThat(commandCaptor.getValue().orderId()).isEqualTo(created.getReferenceId());
        assertThat(commandCaptor.getValue().returnUrl()).isEqualTo(created.getReturnUrl());
    }

    @Test
    @WithJeapAuthenticationToken(userRoles = WRITE_ROLE)
    void retryPayment_cancelledPayment_thenCreated() throws Exception {
        var previous = savePayment(ENTITY_A, "P-00000002");
        previous.markAsCancelled();
        repos.payment.saveAndFlush(previous);

        mockMvc
            .perform(
                MockMvcRequestBuilders.post(RETRY_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(retryRequest(previous.getId(), ENTITY_A)))
            )
            .andExpect(status().isCreated());

        assertThat(repos.payment.findAll()).hasSize(2);
    }

    @Test
    @WithJeapAuthenticationToken(userRoles = WRITE_ROLE)
    void retryPayment_pendingPayment_thenConflict() throws Exception {
        var previous = savePayment(ENTITY_A, "P-00000003");

        mockMvc
            .perform(
                MockMvcRequestBuilders.post(RETRY_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(retryRequest(previous.getId(), ENTITY_A)))
            )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.errorCode").value("payment_not_retryable"));

        assertThat(repos.payment.findAll()).hasSize(1);
        verify(saferpayClient, never()).initialize(any());
    }

    @Test
    @WithJeapAuthenticationToken(userRoles = WRITE_ROLE)
    void retryPayment_succeededPayment_thenConflict() throws Exception {
        var previous = savePayment(ENTITY_A, "P-00000004");
        previous.markAsSucceeded("txn-123", Instant.now(), "VISA", "Cardholder", true);
        repos.payment.saveAndFlush(previous);

        mockMvc
            .perform(
                MockMvcRequestBuilders.post(RETRY_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(retryRequest(previous.getId(), ENTITY_A)))
            )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.errorCode").value("payment_not_retryable"));

        assertThat(repos.payment.findAll()).hasSize(1);
        verify(saferpayClient, never()).initialize(any());
    }

    @Test
    @WithJeapAuthenticationToken(userRoles = WRITE_ROLE)
    void retryPayment_unknownPayment_thenNotFound() throws Exception {
        mockMvc
            .perform(
                MockMvcRequestBuilders.post(RETRY_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(retryRequest(UUID.randomUUID(), ENTITY_A)))
            )
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errorCode").value("resource_not_found"));
    }

    @Test
    @WithExtendedJeapAuthenticationToken(bpRoles = ENTITY_B_S + "=" + WRITE_ROLE)
    void retryPayment_paymentOfOtherPartner_thenNotFound() throws Exception {
        var previous = failedPayment(ENTITY_A, "P-00000005");

        mockMvc
            .perform(
                MockMvcRequestBuilders.post(RETRY_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(retryRequest(previous.getId(), ENTITY_B)))
            )
            .andExpect(status().isNotFound());

        assertThat(repos.payment.findAll()).hasSize(1);
    }

    @Test
    @WithExtendedJeapAuthenticationToken(bpRoles = ENTITY_B_S + "=" + WRITE_ROLE)
    void retryPayment_roleForOtherPartner_thenForbidden() throws Exception {
        var previous = failedPayment(ENTITY_A, "P-00000006");

        mockMvc
            .perform(
                MockMvcRequestBuilders.post(RETRY_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(retryRequest(previous.getId(), ENTITY_A)))
            )
            .andExpect(status().isForbidden());

        assertThat(repos.payment.findAll()).hasSize(1);
    }

    @Test
    @WithJeapAuthenticationToken
    void retryPayment_missingRole_thenForbidden() throws Exception {
        var previous = failedPayment(ENTITY_A, "P-00000007");

        mockMvc
            .perform(
                MockMvcRequestBuilders.post(RETRY_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(retryRequest(previous.getId(), ENTITY_A)))
            )
            .andExpect(status().isForbidden());
    }

    @Test
    @WithJeapAuthenticationToken(userRoles = WRITE_ROLE)
    void retryPayment_pendingPaymentForProductExists_thenBadRequest() throws Exception {
        // the previous payment failed, but the user already started a new one for the same product
        var previous = failedPayment(ENTITY_A, "P-00000008");
        savePayment(ENTITY_A, "P-00000009");

        mockMvc
            .perform(
                MockMvcRequestBuilders.post(RETRY_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(retryRequest(previous.getId(), ENTITY_A)))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("payment_already_pending"));

        verify(saferpayClient, never()).initialize(any());
    }

    private Payment failedPayment(UUID businessPartnerId, String referenceId) {
        var previous = savePayment(businessPartnerId, referenceId);
        previous.markAsFailed();
        return repos.payment.saveAndFlush(previous);
    }

    private PaymentRetryRequestDto retryRequest(UUID previousPaymentId, UUID businessPartnerId) {
        return new PaymentRetryRequestDto(previousPaymentId, businessPartnerId);
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
