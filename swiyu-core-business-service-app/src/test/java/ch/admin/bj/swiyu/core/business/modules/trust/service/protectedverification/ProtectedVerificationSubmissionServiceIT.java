package ch.admin.bj.swiyu.core.business.modules.trust.service.protectedverification;

import static ch.admin.bj.swiyu.core.business.test.ProtectedVerificationSubmissionTestData.protectedVerificationSubmissionRequestDto;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

import ch.admin.bit.jeap.security.test.WithJeapAuthenticationToken;
import ch.admin.bj.swiyu.core.business.common.audit.AuditPublisher;
import ch.admin.bj.swiyu.core.business.common.exceptions.PartnerIsNotTrustedException;
import ch.admin.bj.swiyu.core.business.common.exceptions.ResourceNotFoundException;
import ch.admin.bj.swiyu.core.business.modules.identifier.service.IdentifierEntryService;
import ch.admin.bj.swiyu.core.business.modules.management.service.BusinessPartnerService;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.protectedverification.ProtectedVerificationSubmissionDomainService;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.protectedverification.ProtectedVerificationSubmissionStatus;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.publisher.DomainEventPublisher;
import ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData;
import ch.admin.bj.swiyu.core.business.test.DataJpaTestConfiguration;
import ch.admin.bj.swiyu.core.business.test.ProtectedVerificationSubmissionTestData;
import ch.admin.bj.swiyu.core.business.test.TestRepositories;
import ch.admin.bj.swiyu.core.business.test.container.WithAllTestContainerInitializers;
import ch.admin.bj.swiyu.messagetype.ti.TiProtectedVerificationSubmissionAcceptedEvent;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ActiveProfiles("test")
@DataJpaTest
@WithAllTestContainerInitializers
@Import(
    {
        DataJpaTestConfiguration.class,
        ProtectedVerificationSubmissionService.class,
        ProtectedVerificationSubmissionDomainService.class,
        BusinessPartnerService.class,
    }
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@WithJeapAuthenticationToken(username = "test")
@MockitoBean(types = AuditPublisher.class)
class ProtectedVerificationSubmissionServiceIT {

    @MockitoBean
    DomainEventPublisher domainEventPublisher;

    @MockitoBean
    IdentifierEntryService identifierEntryService;

    @Autowired
    ProtectedVerificationSubmissionService service;

    @Autowired
    TestRepositories testRepositories;

    private final UUID trustedPartnerId = BusinessEntityTestData.DEFAULT_ENTITY;

    @BeforeEach
    void setUp() {
        testRepositories.truncateTables();
        BusinessEntityTestData.insertTestBusinessPartners(testRepositories.businessPartner);
        var trustedEntity = testRepositories.businessPartner.findById(trustedPartnerId).orElseThrow();
        trustedEntity.applyBusinessPartnerIdentityEvent(BusinessEntityTestData.activeBusinessPartnerIdentity());
        testRepositories.businessPartner.save(trustedEntity);
    }

    @Test
    void createProtectedVerificationSubmission_partnerIsTrusted_persistsAndPublishesEvent() {
        doNothing()
            .when(domainEventPublisher)
            .publishProtectedVerificationSubmissionAcceptedEvent(
                any(TiProtectedVerificationSubmissionAcceptedEvent.class)
            );

        var dto = protectedVerificationSubmissionRequestDto(trustedPartnerId);

        var result = service.createProtectedVerificationSubmission(dto);

        assertThat(result).isNotNull();
        assertThat(result.partnerId()).isEqualTo(trustedPartnerId);
        assertThat(result.status()).isEqualTo(
            ch.admin.bj.swiyu.core.business.modules.trust.api.ProtectedVerificationSubmissionStatusDto.SUBMITTED
        );

        var persisted = testRepositories.protectedVerificationSubmission.findById(result.id()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(ProtectedVerificationSubmissionStatus.SUBMITTED);

        verify(domainEventPublisher).publishProtectedVerificationSubmissionAcceptedEvent(any());
    }

    @Test
    void createProtectedVerificationSubmission_partnerNotTrusted_throwsAndDoesNotPersist() {
        var untrustedPartnerId = BusinessEntityTestData.ENTITY_B;
        var dto = ProtectedVerificationSubmissionTestData.protectedVerificationSubmissionRequestDto(untrustedPartnerId);

        assertThatThrownBy(() -> service.createProtectedVerificationSubmission(dto)).isInstanceOf(
            PartnerIsNotTrustedException.class
        );
        assertThat(testRepositories.protectedVerificationSubmission.count()).isZero();
    }

    @Test
    void getProtectedVerificationSubmission_notFound_throws() {
        var nonExistentId = UUID.randomUUID();

        assertThatThrownBy(() -> service.getProtectedVerificationSubmission(nonExistentId)).isInstanceOf(
            ResourceNotFoundException.class
        );
    }

    @Test
    void markAsApproved_updatesStatus() {
        var submission = testRepositories.protectedVerificationSubmission.save(
            ProtectedVerificationSubmissionTestData.protectedVerificationSubmission(trustedPartnerId)
        );

        service.markAsApproved(submission.getId());

        var persisted = testRepositories.protectedVerificationSubmission.findById(submission.getId()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(ProtectedVerificationSubmissionStatus.APPROVED);
    }

    @Test
    void markAsRejected_updatesStatusAndReason() {
        var submission = testRepositories.protectedVerificationSubmission.save(
            ProtectedVerificationSubmissionTestData.protectedVerificationSubmission(trustedPartnerId)
        );
        var rejectReason = "Business partner no longer trusted";

        service.markAsRejected(submission.getId(), rejectReason);

        var persisted = testRepositories.protectedVerificationSubmission.findById(submission.getId()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(ProtectedVerificationSubmissionStatus.REJECTED);
        assertThat(persisted.getRejectReason()).isEqualTo(rejectReason);
    }
}
