package ch.admin.bj.swiyu.core.business.modules.trust.infrastructure.web;

import static ch.admin.bj.swiyu.core.business.test.VqpsSubmissionTestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import ch.admin.bj.swiyu.core.business.common.api.ApiErrorCodeDto;
import ch.admin.bj.swiyu.core.business.common.api.ApiErrorDto;
import ch.admin.bj.swiyu.core.business.modules.trust.api.VqpsSubmissionB2BDto;
import ch.admin.bj.swiyu.core.business.modules.trust.api.VqpsSubmissionCreateRequestDto;
import ch.admin.bj.swiyu.core.business.modules.trust.api.VqpsSubmissionStatusDto;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.publisher.DomainEventPublisher;
import ch.admin.bj.swiyu.core.business.modules.trust.infrastructure.consumer.VqpsPublicationEventConsumer;
import ch.admin.bj.swiyu.core.business.test.TestRepositories;
import ch.admin.bj.swiyu.core.business.test.WithExtendedJeapAuthenticationToken;
import ch.admin.bj.swiyu.core.business.test.container.WithAllTestContainerInitializers;
import ch.admin.bj.swiyu.messagetype.ti.TiVqpsSubmissionAcceptedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@WithAllTestContainerInitializers
@EmbeddedKafka
class VqpsSubmissionB2BControllerIT {

    private static final String VQPS_SUBMISSIONS_B2B_BASE_URL = "/api/v1/trust/";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestRepositories repos;

    @Autowired
    private VqpsPublicationEventConsumer eventConsumer;

    @MockitoBean
    private DomainEventPublisher domainEventPublisher;

    @Autowired
    private TaskScheduler taskScheduler;

    @BeforeEach
    void setUp() {
        repos.vqpsSubmission.deleteAllInBatch();
    }

    @Test
    @WithExtendedJeapAuthenticationToken(
        subject = "deadbeef-0000-0000-0000-000000000000",
        bpRoles = "deadbeef-0000-0000-0000-000000000000 = ti_@vqpssubmission_#write"
    )
    void createVqpsSubmission() throws Exception {
        // GIVEN
        var request = vqpsSubmissionCreateRequestDto();
        mockVqpsPublicationSucceededEvent();

        // WHEN
        var response = executeCreateVqpsSubmission(request);

        // THEN
        assertThat(response.getResponse().getStatus()).isEqualTo(HttpStatus.CREATED.value());
        var vqpsSubmission = objectMapper.readValue(
            response.getResponse().getContentAsString(),
            VqpsSubmissionB2BDto.class
        );
        assertThat(vqpsSubmission).isNotNull();
        assertThat(vqpsSubmission.status()).isEqualTo(VqpsSubmissionStatusDto.PUBLICATION_SUCCEEDED);
        assertThat(vqpsSubmission.publicationResult()).isNotNull();
        assertThat(vqpsSubmission.publicationResult().expiresAt()).isNotNull();
        assertThat(vqpsSubmission.publicationResult().jti()).isNotNull();
        assertThat(vqpsSubmission.publicationResult().jwt()).isNotNull();
    }

    @Test
    @WithExtendedJeapAuthenticationToken(
        subject = "deadbeef-0000-0000-0000-000000000000",
        bpRoles = "deadbeef-0000-0000-0000-000000000000 = ti_@vqpssubmission_#write"
    )
    void createVqpsSubmission_withBeanValidationViolation_returnsBadRequest() throws Exception {
        // GIVEN
        var request = vqpsSubmissionCreateRequestDto();
        var requestWithoutDefault = new VqpsSubmissionCreateRequestDto(
            true,
            request.sub(),
            Map.of("de-CH", "purpose name de", "fr-CH", "purpose name fr"),
            request.purposeDescription(),
            request.scope(),
            request.query()
        );

        // WHEN
        var response = executeCreateVqpsSubmission(requestWithoutDefault);

        // THEN
        assertThat(response.getResponse().getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(repos.vqpsSubmission.count()).isZero();
    }

    @Test
    @WithExtendedJeapAuthenticationToken(
        subject = "deadbeef-0000-0000-0000-000000000000",
        bpRoles = "deadbeef-0000-0000-0000-000000000000 = ti_@vqpssubmission_#write"
    )
    void createVqpsSubmission_too_long_purpose_name() throws Exception {
        // GIVEN
        var request = vqpsSubmissionCreateRequestDto();
        var tooLongValue = "a".repeat(41);
        var requestWithTooLongName = new VqpsSubmissionCreateRequestDto(
            true,
            request.sub(),
            Map.of("default", tooLongValue),
            request.purposeDescription(),
            request.scope(),
            request.query()
        );

        // WHEN
        var response = executeCreateVqpsSubmission(requestWithTooLongName);

        // THEN
        assertThat(response.getResponse().getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        var error = objectMapper.readValue(response.getResponse().getContentAsString(), ApiErrorDto.class);
        assertThat(error.errorCode()).isEqualTo(ApiErrorCodeDto.DATA_INVALID);
        assertThat(error.message()).contains("Validation failed");
        assertThat(repos.vqpsSubmission.count()).isZero();
    }

    @Test
    @WithExtendedJeapAuthenticationToken(
        subject = "deadbeef-0000-0000-0000-000000000000",
        bpRoles = "deadbeef-0000-0000-0000-000000000000 = ti_@vqpssubmission_#write"
    )
    void createVqpsSubmission_withInvalidDcqlQuery_returnsBadRequest() throws Exception {
        // GIVEN
        var request = requestWithQuery(
            """
            {"credentials": [{"meta": {"vct_values": []}}]}
            """
        );

        // WHEN
        var response = executeCreateVqpsSubmission(request);

        // THEN
        assertThat(response.getResponse().getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        var error = objectMapper.readValue(response.getResponse().getContentAsString(), ApiErrorDto.class);
        assertThat(error.errorCode()).isEqualTo(ApiErrorCodeDto.DATA_INVALID);
        assertThat(error.message()).contains("Invalid DCQL query");
        assertThat(repos.vqpsSubmission.count()).isZero();
    }

    @Transactional
    @Test
    @WithExtendedJeapAuthenticationToken(
        subject = "deadbeef-0000-0000-0000-000000000000",
        bpRoles = "deadbeef-0000-0000-0000-000000000000 = ti_@vqpssubmission_#read"
    )
    void getVqpsSubmission() throws Exception {
        // GIVEN
        var partnerId = UUID.fromString("deadbeef-0000-0000-0000-000000000000");
        var submission = repos.vqpsSubmission.save(vqpsSubmission(partnerId));
        repos.commit();

        // WHEN
        var response = executeGetVqpsSubmission(submission.getId());

        // THEN
        assertThat(response.getResponse().getStatus()).isEqualTo(HttpStatus.OK.value());
        var result = objectMapper.readValue(response.getResponse().getContentAsString(), VqpsSubmissionB2BDto.class);
        assertThat(result.status()).isEqualTo(VqpsSubmissionStatusDto.ACCEPTED);
        assertThat(result.publicationResult()).isNull();
        assertThat(result.publicationFailureReason()).isNull();
    }

    @Transactional
    @Test
    @WithExtendedJeapAuthenticationToken(
        subject = "deadbeef-0000-0000-0000-000000000000",
        bpRoles = "deadbeef-0000-0000-0000-000000000000 = ti_@vqpssubmission_#read"
    )
    void getVqpsSubmissions() throws Exception {
        // GIVEN
        var partnerId = UUID.fromString("deadbeef-0000-0000-0000-000000000000");
        repos.vqpsSubmission.saveAll(
            List.of(vqpsSubmission(partnerId), vqpsSubmission(partnerId), vqpsSubmission(UUID.randomUUID()))
        );
        repos.commit();

        // WHEN
        var response = executeGetVqpsSubmissions();

        // THEN
        assertThat(response.getResponse().getStatus()).isEqualTo(HttpStatus.OK.value());
        var submissionsJson = objectMapper.readTree(response.getResponse().getContentAsString());
        assertThat(submissionsJson).isNotNull();
        assertThat(submissionsJson.get("page").get("totalElements").asInt()).isEqualTo(2);
        assertThat(submissionsJson.get("content")).hasSize(2);
    }

    private @NonNull MvcResult executeCreateVqpsSubmission(VqpsSubmissionCreateRequestDto request) throws Exception {
        return mockMvc
            .perform(
                MockMvcRequestBuilders.post(VQPS_SUBMISSIONS_B2B_BASE_URL + "vqps-submissions")
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andReturn();
    }

    private @NonNull MvcResult executeGetVqpsSubmission(UUID submissionId) throws Exception {
        return mockMvc
            .perform(
                MockMvcRequestBuilders.get(
                    VQPS_SUBMISSIONS_B2B_BASE_URL + "vqps-submissions/{id}",
                    submissionId
                ).contentType(MediaType.APPLICATION_JSON)
            )
            .andReturn();
    }

    private @NonNull MvcResult executeGetVqpsSubmissions() throws Exception {
        return mockMvc
            .perform(
                MockMvcRequestBuilders.get(VQPS_SUBMISSIONS_B2B_BASE_URL + "vqps-submissions").contentType(
                    MediaType.APPLICATION_JSON
                )
            )
            .andReturn();
    }

    /**
     * Sends a TiVqpsPublicationSucceededEvent to VqpsPublicationEventConsumer once the TiVqpsSubmissionAcceptedEvent
     * was published.
     */
    private void mockVqpsPublicationSucceededEvent() {
        doAnswer(invocation -> {
            TiVqpsSubmissionAcceptedEvent event = invocation.getArgument(0);
            var submissionId = event.getPayload().getVqpsSubmissionId();
            runWithDelayAfterTransactionCommit(() -> {
                log.debug(
                    "Simulating VQPS publication succeeded event for submission id {} after transaction commit",
                    submissionId
                );
                eventConsumer.receive(tiVqpsPublicationSucceededEvent(submissionId), fakeAcknowledgement());
            });
            return null;
        })
            .when(domainEventPublisher)
            .publishVqpsSubmissionAcceptedEvent(any());
    }

    /**
     * Waits until the transaction closes and then schedules the given task with a delay of 100 ms.
     */
    private void runWithDelayAfterTransactionCommit(Runnable task) {
        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    taskScheduler.schedule(task, Instant.now().plusMillis(1));
                }
            }
        );
    }

    private static @NonNull Acknowledgment fakeAcknowledgement() {
        return () -> {};
    }

    private VqpsSubmissionCreateRequestDto requestWithQuery(String queryJson) throws JsonProcessingException {
        var base = vqpsSubmissionCreateRequestDto();
        JsonNode query = objectMapper.readTree(queryJson);
        return vqpsSubmissionCreateRequestDto(
            base.sub(),
            base.purposeName(),
            base.purposeDescription(),
            base.scope(),
            query
        );
    }
}
