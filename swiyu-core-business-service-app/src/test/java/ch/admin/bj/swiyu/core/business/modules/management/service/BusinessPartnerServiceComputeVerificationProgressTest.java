package ch.admin.bj.swiyu.core.business.modules.management.service;

import static ch.admin.bj.swiyu.core.business.modules.management.api.IdentityVerificationProgressStatusDto.*;
import static ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustOnboardingSubmissionStatus.*;
import static ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustOnboardingSubmissionType.*;
import static ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData.*;
import static ch.admin.bj.swiyu.core.business.test.TrustOnboardingSubmissionTestData.trustOnboardingSubmission;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import ch.admin.bj.swiyu.core.business.common.audit.AuditPublisher;
import ch.admin.bj.swiyu.core.business.modules.identifier.service.IdentifierEntryService;
import ch.admin.bj.swiyu.core.business.modules.management.domain.BusinessEntity;
import ch.admin.bj.swiyu.core.business.modules.management.domain.BusinessPartnerIdentityRepository;
import ch.admin.bj.swiyu.core.business.modules.management.domain.BusinessPartnerRepository;
import ch.admin.bj.swiyu.core.business.modules.management.domain.pams.PamsClient;
import ch.admin.bj.swiyu.core.business.modules.trust.config.TrustOnboardingSubmissionLimitProperties;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustOnboardingSubmission;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustOnboardingSubmissionRepository;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustOnboardingSubmissionStatus;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustOnboardingSubmissionType;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.publisher.DomainEventPublisher;
import ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BusinessPartnerServiceComputeVerificationProgressTest {

    private BusinessPartnerService businessPartnerService = new BusinessPartnerService(
        mock(BusinessPartnerRepository.class),
        mock(BusinessPartnerIdentityRepository.class),
        mock(TrustOnboardingSubmissionRepository.class),
        mock(TrustOnboardingSubmissionLimitProperties.class),
        mock(PamsClient.class),
        mock(IdentifierEntryService.class),
        mock(AuditPublisher.class),
        mock(DomainEventPublisher.class)
    );

    // -------------------------------------------------------------------------
    // No BPI — first-time verification
    // -------------------------------------------------------------------------
    @Test
    void noBpi_noActiveSubmission_returnsVerificationNotStarted() {
        // Given
        var partner = businessPartnerA();
        List<TrustOnboardingSubmission> noSubmissions = emptyList();
        // When
        var progress = businessPartnerService.computeVerificationProgress(partner, noSubmissions);
        // Then
        assertThat(progress).isNotNull();
        assertThat(progress.status()).isEqualTo(VERIFICATION_NOT_STARTED);
        assertThat(progress.maxDateForStatus()).isNull();
    }

    @Test
    void noBpi_onlyExpiredSubmission_returnsVerificationNotStarted() {
        // Given
        var partner = businessPartnerA();
        var submissions = List.of(submission(REGISTRATION, UNSUBMITTED_TIMEOUT));
        // When
        var progress = businessPartnerService.computeVerificationProgress(partner, submissions);
        // Then
        assertThat(progress.status()).isEqualTo(VERIFICATION_NOT_STARTED);
        assertThat(progress.maxDateForStatus()).isNull();
    }

    @Test
    void noBpi_lastSubmissionRejected_returnsVerificationRejected() {
        // Given
        var partner = businessPartnerA();
        var submissions = List.of(submission(REGISTRATION, REJECTED));
        // When
        var progress = businessPartnerService.computeVerificationProgress(partner, submissions);
        // Then
        assertThat(progress.status()).isEqualTo(VERIFICATION_REJECTED);
        assertThat(progress.maxDateForStatus()).isNull();
    }

    @Test
    void noBpi_latestSubmissionUnsubmitted_returnsVerificationStarted() {
        // Given
        var partner = businessPartnerA();
        var submissions = List.of(submission(REGISTRATION, UNSUBMITTED));
        // When
        var progress = businessPartnerService.computeVerificationProgress(partner, submissions);
        // Then
        assertThat(progress.status()).isEqualTo(VERIFICATION_STARTED);
        assertThat(progress.maxDateForStatus()).isNotNull();
    }

    @Test
    void noBpi_latestSubmissionUnsubmittedWithSubmittedAt_returnsVerificationInformationRequestedStarted() {
        // Given
        var partner = businessPartnerA();
        var submissions = List.of(submissionUnsubmittedWithSubmittedAt());
        // When
        var progress = businessPartnerService.computeVerificationProgress(partner, submissions);
        // Then
        assertThat(progress.status()).isEqualTo(VERIFICATION_INFORMATION_REQUESTED_STARTED);
        assertThat(progress.maxDateForStatus()).isNotNull();
    }

    @Test
    void noBpi_latestSubmissionSubmitted_returnsVerificationInProgress() {
        // Given
        var partner = businessPartnerA();
        var submissions = List.of(submission(REGISTRATION, SUBMITTED));
        // When
        var progress = businessPartnerService.computeVerificationProgress(partner, submissions);
        // Then
        assertThat(progress.status()).isEqualTo(VERIFICATION_IN_PROGRESS);
        assertThat(progress.maxDateForStatus()).isNull();
    }

    @Test
    void noBpi_latestSubmissionResubmitted_returnsVerificationInProgress() {
        // Given
        var partner = businessPartnerA();
        var submissions = List.of(submission(REGISTRATION, RESUBMITTED));
        // When
        var progress = businessPartnerService.computeVerificationProgress(partner, submissions);
        // Then
        assertThat(progress.status()).isEqualTo(VERIFICATION_IN_PROGRESS);
        assertThat(progress.maxDateForStatus()).isNotNull();
    }

    @Test
    void noBpi_latestSubmissionInformationRequested_returnsVerificationInformationRequestedRequired() {
        // Given
        var partner = businessPartnerA();
        var submissions = List.of(submission(REGISTRATION, INFORMATION_REQUESTED));
        // When
        var progress = businessPartnerService.computeVerificationProgress(partner, submissions);
        // Then
        assertThat(progress.status()).isEqualTo(VERIFICATION_INFORMATION_REQUESTED_REQUIRED);
        assertThat(progress.maxDateForStatus()).isNotNull();
    }

    // Migration window (no BPI yet, TMS event not received): a terminal SUCCEEDED with no ongoing
    // submission is still treated as verified. Not part of the DTO derivation table, but documented
    // behavior in BusinessPartnerService.computeVerificationProgress.
    @Test
    void noBpi_succeededSubmission_returnsVerificationSucceeded() {
        // Given
        var partner = businessPartnerA();
        var submissions = List.of(submission(REGISTRATION, SUCCEEDED));
        // When
        var progress = businessPartnerService.computeVerificationProgress(partner, submissions);
        // Then
        assertThat(progress.status()).isEqualTo(VERIFICATION_SUCCEEDED);
        assertThat(progress.maxDateForStatus()).isNull();
    }

    // -------------------------------------------------------------------------
    // BPI ACTIVE
    // -------------------------------------------------------------------------

    @Test
    void activeBpi_noActiveSubmission_returnsVerificationSucceeded() {
        // Given
        var partner = partnerWithActivatedIdentity();
        List<TrustOnboardingSubmission> submissions = emptyList();
        // When
        var progress = businessPartnerService.computeVerificationProgress(partner, submissions);
        // Then
        assertThat(progress.status()).isEqualTo(VERIFICATION_SUCCEEDED);
        assertThat(progress.maxDateForStatus()).isNull();
    }

    @Test
    void activeBpi_succeededThenExpired_returnsVerificationSucceeded() {
        // Given
        var partner = partnerWithActivatedIdentity();
        var submissions = List.of(submission(REGISTRATION, SUCCEEDED), submission(REGISTRATION, UNSUBMITTED_TIMEOUT));
        // When
        var progress = businessPartnerService.computeVerificationProgress(partner, submissions);
        // Then
        assertThat(progress.status()).isEqualTo(VERIFICATION_SUCCEEDED);
        assertThat(progress.maxDateForStatus()).isNull();
    }

    @Test
    void activeBpi_latestSubmissionUnsubmitted_returnsReVerificationStarted() {
        // Given
        var partner = partnerWithActivatedIdentity();
        var submissions = List.of(submission(REGISTRATION, SUCCEEDED), submission(REGISTRATION, UNSUBMITTED));
        // When
        var progress = businessPartnerService.computeVerificationProgress(partner, submissions);
        // Then
        assertThat(progress.status()).isEqualTo(RE_VERIFICATION_STARTED);
        assertThat(progress.maxDateForStatus()).isNotNull();
    }

    @Test
    void activeBpi_latestSubmissionUnsubmittedCloseToLimit_returnsReVerificationStarted() {
        // Given
        var partner = partnerWithActivatedIdentity();
        var submissions = List.of(submission(REGISTRATION, SUCCEEDED), submissionUnsubmittedCloseToLimit());
        // When
        var progress = businessPartnerService.computeVerificationProgress(partner, submissions);
        // Then
        assertThat(progress.status()).isEqualTo(RE_VERIFICATION_STARTED);
        assertThat(progress.maxDateForStatus()).isNotNull();
    }

    @Test
    void activeBpi_latestSubmissionProfileChangeMandatoryUnsubmitted_returnsReVerificationStarted() {
        // Given
        var partner = partnerWithActivatedIdentity();
        var submissions = List.of(
            submission(REGISTRATION, SUCCEEDED),
            submission(PROFILE_CHANGE_MANDATORY, UNSUBMITTED)
        );
        // When
        var progress = businessPartnerService.computeVerificationProgress(partner, submissions);
        // Then
        assertThat(progress.status()).isEqualTo(RE_VERIFICATION_STARTED);
        assertThat(progress.maxDateForStatus()).isNotNull();
    }

    @Test
    void activeBpi_latestSubmissionProfileChangeVoluntaryUnsubmitted_returnsReVerificationStarted() {
        // Given
        var partner = partnerWithActivatedIdentity();
        var submissions = List.of(
            submission(REGISTRATION, SUCCEEDED),
            submission(PROFILE_CHANGE_VOLUNTARY, UNSUBMITTED)
        );
        // When
        var progress = businessPartnerService.computeVerificationProgress(partner, submissions);
        // Then
        assertThat(progress.status()).isEqualTo(RE_VERIFICATION_STARTED);
        assertThat(progress.maxDateForStatus()).isNotNull();
    }

    @Test
    void activeBpi_latestSubmissionRenewalUnsubmitted_returnsReVerificationStarted() {
        // Given
        var partner = partnerWithActivatedIdentity();
        var submissions = List.of(submission(REGISTRATION, SUCCEEDED), submission(RENEWAL, UNSUBMITTED));
        // When
        var progress = businessPartnerService.computeVerificationProgress(partner, submissions);
        // Then
        assertThat(progress.status()).isEqualTo(RE_VERIFICATION_STARTED);
        assertThat(progress.maxDateForStatus()).isNotNull();
    }

    @Test
    void activeBpi_latestSubmissionUnsubmittedWithSubmittedAt_returnsVerificationInformationRequestedStarted() {
        // Given
        var partner = partnerWithActivatedIdentity();
        var submissions = List.of(submission(REGISTRATION, SUCCEEDED), submissionUnsubmittedWithSubmittedAt());
        // When
        var progress = businessPartnerService.computeVerificationProgress(partner, submissions);
        // Then
        assertThat(progress.status()).isEqualTo(VERIFICATION_INFORMATION_REQUESTED_STARTED);
        assertThat(progress.maxDateForStatus()).isNotNull();
    }

    @Test
    void activeBpi_latestSubmissionSubmitted_returnsReVerificationInProgress() {
        // Given
        var partner = partnerWithActivatedIdentity();
        var submissions = List.of(submission(REGISTRATION, SUCCEEDED), submission(REGISTRATION, SUBMITTED));
        // When
        var progress = businessPartnerService.computeVerificationProgress(partner, submissions);
        // Then
        assertThat(progress.status()).isEqualTo(RE_VERIFICATION_IN_PROGRESS);
        assertThat(progress.maxDateForStatus()).isNull();
    }

    @Test
    void activeBpi_latestSubmissionInformationRequested_returnsVerificationInformationRequestedRequired() {
        // Given
        var partner = partnerWithActivatedIdentity();
        var submissions = List.of(submission(REGISTRATION, SUCCEEDED), submission(REGISTRATION, INFORMATION_REQUESTED));
        // When
        var progress = businessPartnerService.computeVerificationProgress(partner, submissions);
        // Then
        assertThat(progress.status()).isEqualTo(VERIFICATION_INFORMATION_REQUESTED_REQUIRED);
        assertThat(progress.maxDateForStatus()).isNotNull();
    }

    @Test
    void activeBpi_latestSubmissionResubmitted_returnsReVerificationInProgress() {
        // Given
        var partner = partnerWithActivatedIdentity();
        var submissions = List.of(submission(REGISTRATION, SUCCEEDED), submission(REGISTRATION, RESUBMITTED));
        // When
        var progress = businessPartnerService.computeVerificationProgress(partner, submissions);
        // Then
        assertThat(progress.status()).isEqualTo(RE_VERIFICATION_IN_PROGRESS);
        assertThat(progress.maxDateForStatus()).isNotNull();
    }

    @Test
    void activeBpi_latestSubmissionProfileChangeMandatoryResubmitted_returnsReVerificationInProgress() {
        // Given
        var partner = partnerWithActivatedIdentity();
        var submissions = List.of(
            submission(REGISTRATION, SUCCEEDED),
            submission(PROFILE_CHANGE_MANDATORY, RESUBMITTED)
        );
        // When
        var progress = businessPartnerService.computeVerificationProgress(partner, submissions);
        // Then
        assertThat(progress.status()).isEqualTo(RE_VERIFICATION_IN_PROGRESS);
        assertThat(progress.maxDateForStatus()).isNotNull();
    }

    @Test
    void activeBpi_latestSubmissionProfileChangeVoluntaryResubmitted_returnsReVerificationInProgress() {
        // Given
        var partner = partnerWithActivatedIdentity();
        var submissions = List.of(
            submission(REGISTRATION, SUCCEEDED),
            submission(PROFILE_CHANGE_VOLUNTARY, RESUBMITTED)
        );
        // When
        var progress = businessPartnerService.computeVerificationProgress(partner, submissions);
        // Then
        assertThat(progress.status()).isEqualTo(RE_VERIFICATION_IN_PROGRESS);
        assertThat(progress.maxDateForStatus()).isNotNull();
    }

    @Test
    void activeBpi_latestSubmissionRenewalResubmitted_returnsReVerificationInProgress() {
        // Given
        var partner = partnerWithActivatedIdentity();
        var submissions = List.of(submission(REGISTRATION, SUCCEEDED), submission(RENEWAL, RESUBMITTED));
        // When
        var progress = businessPartnerService.computeVerificationProgress(partner, submissions);
        // Then
        assertThat(progress.status()).isEqualTo(RE_VERIFICATION_IN_PROGRESS);
        assertThat(progress.maxDateForStatus()).isNotNull();
    }

    @Test
    void activeBpi_lastSubmissionRejected_returnsReVerificationRejected() {
        // Given
        var partner = partnerWithActivatedIdentity();
        var submissions = List.of(submission(REGISTRATION, SUCCEEDED), submission(REGISTRATION, REJECTED));
        // When
        var progress = businessPartnerService.computeVerificationProgress(partner, submissions);
        // Then
        assertThat(progress.status()).isEqualTo(RE_VERIFICATION_REJECTED);
        assertThat(progress.maxDateForStatus()).isNull();
    }

    @Test
    void activeBpi_lastSubmissionProfileChangeMandatoryRejected_returnsReVerificationRejected() {
        // Given
        var partner = partnerWithActivatedIdentity();
        var submissions = List.of(submission(REGISTRATION, SUCCEEDED), submission(PROFILE_CHANGE_MANDATORY, REJECTED));
        // When
        var progress = businessPartnerService.computeVerificationProgress(partner, submissions);
        // Then
        assertThat(progress.status()).isEqualTo(RE_VERIFICATION_REJECTED);
        assertThat(progress.maxDateForStatus()).isNull();
    }

    @Test
    void activeBpi_lastSubmissionProfileChangeVoluntaryRejected_returnsReVerificationRejected() {
        // Given
        var partner = partnerWithActivatedIdentity();
        var submissions = List.of(submission(REGISTRATION, SUCCEEDED), submission(PROFILE_CHANGE_VOLUNTARY, REJECTED));
        // When
        var progress = businessPartnerService.computeVerificationProgress(partner, submissions);
        // Then
        assertThat(progress.status()).isEqualTo(RE_VERIFICATION_REJECTED);
        assertThat(progress.maxDateForStatus()).isNull();
    }

    @Test
    void activeBpi_latestSubmissionProfileChangeMandatorySucceeded_returnsReVerificationSucceeded() {
        // Given
        var partner = partnerWithActivatedIdentity();
        var submissions = List.of(submission(REGISTRATION, SUCCEEDED), submission(PROFILE_CHANGE_MANDATORY, SUCCEEDED));
        // When
        var progress = businessPartnerService.computeVerificationProgress(partner, submissions);
        // Then
        assertThat(progress.status()).isEqualTo(RE_VERIFICATION_SUCCEEDED);
        assertThat(progress.maxDateForStatus()).isNotNull();
    }

    @Test
    void activeBpi_latestSubmissionProfileChangeVoluntarySucceeded_returnsReVerificationSucceeded() {
        // Given
        var partner = partnerWithActivatedIdentity();
        var submissions = List.of(submission(REGISTRATION, SUCCEEDED), submission(PROFILE_CHANGE_VOLUNTARY, SUCCEEDED));
        // When
        var progress = businessPartnerService.computeVerificationProgress(partner, submissions);
        // Then
        assertThat(progress.status()).isEqualTo(RE_VERIFICATION_SUCCEEDED);
        assertThat(progress.maxDateForStatus()).isNotNull();
    }

    @Test
    void activeBpi_latestSubmissionRenewalSucceeded_returnsReVerificationSucceeded() {
        // Given
        var partner = partnerWithActivatedIdentity();
        var submissions = List.of(submission(REGISTRATION, SUCCEEDED), submission(RENEWAL, SUCCEEDED));
        // When
        var progress = businessPartnerService.computeVerificationProgress(partner, submissions);
        // Then
        assertThat(progress.status()).isEqualTo(RE_VERIFICATION_SUCCEEDED);
        assertThat(progress.maxDateForStatus()).isNotNull();
    }

    @Test
    void activeBpi_latestSubmissionSucceeded_returnsReVerificationSucceeded() {
        // Given
        var partner = partnerWithActivatedIdentity();
        var submissions = List.of(submission(REGISTRATION, SUCCEEDED), submission(PROFILE_CHANGE_VOLUNTARY, SUCCEEDED));
        // When
        var progress = businessPartnerService.computeVerificationProgress(partner, submissions);
        // Then
        assertThat(progress.status()).isEqualTo(RE_VERIFICATION_SUCCEEDED);
        assertThat(progress.maxDateForStatus()).isNotNull();
    }

    // -------------------------------------------------------------------------
    // BPI ACTIVE but expiring soon (< 90 days) → re-verification required
    // -------------------------------------------------------------------------

    @Test
    void activeBpiExpiringSoon_noActiveSubmission_returnsReVerificationRequired() {
        // Given
        var partner = partnerWithActiveBpiExpiringSoon();
        var submissions = List.of(submission(REGISTRATION, SUCCEEDED));
        // When
        var progress = businessPartnerService.computeVerificationProgress(partner, submissions);
        // Then
        assertThat(progress.status()).isEqualTo(RE_VERIFICATION_REQUIRED);
        assertThat(progress.maxDateForStatus()).isNull();
    }

    // -------------------------------------------------------------------------
    // BPI DEACTIVATED
    // -------------------------------------------------------------------------

    @Test
    void deactivatedBpi_latestSubmissionSucceeded_returnsVerificationNotStarted() {
        // Given
        var partner = partnerWithDeactivatedIdentity();
        var submissions = List.of(submission(REGISTRATION, SUCCEEDED));
        // When
        var progress = businessPartnerService.computeVerificationProgress(partner, submissions);
        // Then
        assertThat(progress.status()).isEqualTo(VERIFICATION_NOT_STARTED);
        assertThat(progress.maxDateForStatus()).isNull();
    }

    @Test
    void deactivatedBpi_latestSubmissionUnsubmittedTimeout_returnsVerificationNotStarted() {
        // Given
        var partner = partnerWithDeactivatedIdentity();
        var submissions = List.of(submission(REGISTRATION, SUCCEEDED), submission(REGISTRATION, UNSUBMITTED_TIMEOUT));
        // When
        var progress = businessPartnerService.computeVerificationProgress(partner, submissions);
        // Then
        assertThat(progress.status()).isEqualTo(VERIFICATION_NOT_STARTED);
        assertThat(progress.maxDateForStatus()).isNull();
    }

    @Test
    void deactivatedBpi_lastSubmissionRejected_returnsReVerificationRejected() {
        // Given
        var partner = partnerWithDeactivatedIdentity();
        var submissions = List.of(submission(REGISTRATION, SUCCEEDED), submission(REGISTRATION, REJECTED));
        // When
        var progress = businessPartnerService.computeVerificationProgress(partner, submissions);
        // Then
        assertThat(progress.status()).isEqualTo(RE_VERIFICATION_REJECTED);
        assertThat(progress.maxDateForStatus()).isNull();
    }

    @Test
    void deactivatedBpi_lastSubmissionProfileChangeMandatoryRejected_returnsReVerificationRejected() {
        // Given
        var partner = partnerWithDeactivatedIdentity();
        var submissions = List.of(submission(REGISTRATION, SUCCEEDED), submission(PROFILE_CHANGE_MANDATORY, REJECTED));
        // When
        var progress = businessPartnerService.computeVerificationProgress(partner, submissions);
        // Then
        assertThat(progress.status()).isEqualTo(RE_VERIFICATION_REJECTED);
        assertThat(progress.maxDateForStatus()).isNull();
    }

    @Test
    void deactivatedBpi_lastSubmissionProfileChangeVoluntaryRejected_returnsReVerificationRejected() {
        // Given
        var partner = partnerWithDeactivatedIdentity();
        var submissions = List.of(submission(REGISTRATION, SUCCEEDED), submission(PROFILE_CHANGE_VOLUNTARY, REJECTED));
        // When
        var progress = businessPartnerService.computeVerificationProgress(partner, submissions);
        // Then
        assertThat(progress.status()).isEqualTo(RE_VERIFICATION_REJECTED);
        assertThat(progress.maxDateForStatus()).isNull();
    }

    @Test
    void deactivatedBpi_latestSubmissionUnsubmitted_returnsReVerificationStarted() {
        // Given
        var partner = partnerWithDeactivatedIdentity();
        var submissions = List.of(submission(REGISTRATION, SUCCEEDED), submission(REGISTRATION, UNSUBMITTED));
        // When
        var progress = businessPartnerService.computeVerificationProgress(partner, submissions);
        // Then
        assertThat(progress.status()).isEqualTo(RE_VERIFICATION_STARTED);
        assertThat(progress.maxDateForStatus()).isNotNull();
    }

    @Test
    void deactivatedBpi_latestSubmissionUnsubmittedCloseToLimit_returnsReVerificationStarted() {
        // Given
        var partner = partnerWithDeactivatedIdentity();
        var submissions = List.of(submission(REGISTRATION, SUCCEEDED), submissionUnsubmittedCloseToLimit());
        // When
        var progress = businessPartnerService.computeVerificationProgress(partner, submissions);
        // Then
        assertThat(progress.status()).isEqualTo(RE_VERIFICATION_STARTED);
        assertThat(progress.maxDateForStatus()).isNotNull();
    }

    @Test
    void deactivatedBpi_latestSubmissionProfileChangeMandatoryUnsubmitted_returnsReVerificationStarted() {
        // Given
        var partner = partnerWithDeactivatedIdentity();
        var submissions = List.of(
            submission(REGISTRATION, SUCCEEDED),
            submission(PROFILE_CHANGE_MANDATORY, UNSUBMITTED)
        );
        // When
        var progress = businessPartnerService.computeVerificationProgress(partner, submissions);
        // Then
        assertThat(progress.status()).isEqualTo(RE_VERIFICATION_STARTED);
        assertThat(progress.maxDateForStatus()).isNotNull();
    }

    @Test
    void deactivatedBpi_latestSubmissionProfileChangeVoluntaryUnsubmitted_returnsReVerificationStarted() {
        // Given
        var partner = partnerWithDeactivatedIdentity();
        var submissions = List.of(
            submission(REGISTRATION, SUCCEEDED),
            submission(PROFILE_CHANGE_VOLUNTARY, UNSUBMITTED)
        );
        // When
        var progress = businessPartnerService.computeVerificationProgress(partner, submissions);
        // Then
        assertThat(progress.status()).isEqualTo(RE_VERIFICATION_STARTED);
        assertThat(progress.maxDateForStatus()).isNotNull();
    }

    @Test
    void deactivatedBpi_latestSubmissionRenewalUnsubmitted_returnsReVerificationStarted() {
        // Given
        var partner = partnerWithDeactivatedIdentity();
        var submissions = List.of(submission(REGISTRATION, SUCCEEDED), submission(RENEWAL, UNSUBMITTED));
        // When
        var progress = businessPartnerService.computeVerificationProgress(partner, submissions);
        // Then
        assertThat(progress.status()).isEqualTo(RE_VERIFICATION_STARTED);
        assertThat(progress.maxDateForStatus()).isNotNull();
    }

    @Test
    void deactivatedBpi_latestSubmissionUnsubmittedWithSubmittedAt_returnsVerificationInformationRequestedStarted() {
        // Given
        var partner = partnerWithDeactivatedIdentity();
        var submissions = List.of(submission(REGISTRATION, SUCCEEDED), submissionUnsubmittedWithSubmittedAt());
        // When
        var progress = businessPartnerService.computeVerificationProgress(partner, submissions);
        // Then
        assertThat(progress.status()).isEqualTo(VERIFICATION_INFORMATION_REQUESTED_STARTED);
        assertThat(progress.maxDateForStatus()).isNotNull();
    }

    @Test
    void deactivatedBpi_latestSubmissionSubmitted_returnsReVerificationInProgress() {
        // Given
        var partner = partnerWithDeactivatedIdentity();
        var submissions = List.of(submission(REGISTRATION, SUCCEEDED), submission(REGISTRATION, SUBMITTED));
        // When
        var progress = businessPartnerService.computeVerificationProgress(partner, submissions);
        // Then
        assertThat(progress.status()).isEqualTo(RE_VERIFICATION_IN_PROGRESS);
        assertThat(progress.maxDateForStatus()).isNull();
    }

    @Test
    void deactivatedBpi_latestSubmissionProfileChangeMandatoryResubmitted_returnsReVerificationInProgress() {
        // Given
        var partner = partnerWithDeactivatedIdentity();
        var submissions = List.of(
            submission(REGISTRATION, SUCCEEDED),
            submission(PROFILE_CHANGE_MANDATORY, RESUBMITTED)
        );
        // When
        var progress = businessPartnerService.computeVerificationProgress(partner, submissions);
        // Then
        assertThat(progress.status()).isEqualTo(RE_VERIFICATION_IN_PROGRESS);
        assertThat(progress.maxDateForStatus()).isNotNull();
    }

    @Test
    void deactivatedBpi_latestSubmissionProfileChangeVoluntaryResubmitted_returnsReVerificationInProgress() {
        // Given
        var partner = partnerWithDeactivatedIdentity();
        var submissions = List.of(
            submission(REGISTRATION, SUCCEEDED),
            submission(PROFILE_CHANGE_VOLUNTARY, RESUBMITTED)
        );
        // When
        var progress = businessPartnerService.computeVerificationProgress(partner, submissions);
        // Then
        assertThat(progress.status()).isEqualTo(RE_VERIFICATION_IN_PROGRESS);
        assertThat(progress.maxDateForStatus()).isNotNull();
    }

    @Test
    void deactivatedBpi_latestSubmissionRenewalResubmitted_returnsReVerificationInProgress() {
        // Given
        var partner = partnerWithDeactivatedIdentity();
        var submissions = List.of(submission(REGISTRATION, SUCCEEDED), submission(RENEWAL, RESUBMITTED));
        // When
        var progress = businessPartnerService.computeVerificationProgress(partner, submissions);
        // Then
        assertThat(progress.status()).isEqualTo(RE_VERIFICATION_IN_PROGRESS);
        assertThat(progress.maxDateForStatus()).isNotNull();
    }

    @Test
    void deactivatedBpi_latestSubmissionResubmitted_returnsReVerificationInProgress() {
        // Given
        var partner = partnerWithDeactivatedIdentity();
        var submissions = List.of(submission(REGISTRATION, SUCCEEDED), submission(REGISTRATION, RESUBMITTED));
        // When
        var progress = businessPartnerService.computeVerificationProgress(partner, submissions);
        // Then
        assertThat(progress.status()).isEqualTo(RE_VERIFICATION_IN_PROGRESS);
        assertThat(progress.maxDateForStatus()).isNotNull();
    }

    @Test
    void deactivatedBpi_latestSubmissionInformationRequested_returnsVerificationInformationRequestedRequired() {
        // Given
        var partner = partnerWithDeactivatedIdentity();
        var submissions = List.of(submission(REGISTRATION, SUCCEEDED), submission(REGISTRATION, INFORMATION_REQUESTED));
        // When
        var progress = businessPartnerService.computeVerificationProgress(partner, submissions);
        // Then
        assertThat(progress.status()).isEqualTo(VERIFICATION_INFORMATION_REQUESTED_REQUIRED);
        assertThat(progress.maxDateForStatus()).isNotNull();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static TrustOnboardingSubmission submission(
        TrustOnboardingSubmissionType type,
        TrustOnboardingSubmissionStatus status
    ) {
        return trustOnboardingSubmission(
            UUID.randomUUID(),
            BusinessEntityTestData.DEFAULT_ENTITY,
            status,
            Instant.now(),
            type
        );
    }

    private static TrustOnboardingSubmission submissionUnsubmittedCloseToLimit() {
        var submission = submission(REGISTRATION, UNSUBMITTED);
        ReflectionTestUtils.setField(submission, "resubmitRequiredUntil", Instant.now().plus(7, ChronoUnit.DAYS));
        return submission;
    }

    private static TrustOnboardingSubmission submissionUnsubmittedWithSubmittedAt() {
        var submission = submission(REGISTRATION, UNSUBMITTED);
        ReflectionTestUtils.setField(submission, "submittedAt", Instant.now().minus(7, ChronoUnit.DAYS));
        return submission;
    }

    private static TrustOnboardingSubmission submissionProfileChangeRenewal(TrustOnboardingSubmissionStatus status) {
        return submission(RENEWAL, status);
    }

    private static BusinessEntity partnerWithActivatedIdentity() {
        var partner = businessPartnerA();
        ReflectionTestUtils.setField(partner, "businessPartnerIdentity", businessPartnerIdentity(partner.getId()));
        return partner;
    }

    private static BusinessEntity partnerWithDeactivatedIdentity() {
        var partner = businessPartnerA();
        ReflectionTestUtils.setField(
            partner,
            "businessPartnerIdentity",
            deactivatedBusinessPartnerIdentity(partner.getId())
        );
        return partner;
    }

    private static BusinessEntity partnerWithActiveBpiExpiringSoon() {
        var partner = businessPartnerA();
        ReflectionTestUtils.setField(
            partner,
            "businessPartnerIdentity",
            businessPartnerIdentity(partner.getId(), Instant.now().plus(30, ChronoUnit.DAYS))
        );
        return partner;
    }
}
