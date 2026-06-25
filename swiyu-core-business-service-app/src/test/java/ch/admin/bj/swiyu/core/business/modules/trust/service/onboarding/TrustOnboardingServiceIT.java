package ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding;

import static ch.admin.bj.swiyu.core.business.modules.documents.api.PartnerDocumentTypeDto.TRUST_ONBOARDING_DECLARATION_OF_INTENT;
import static ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding.ProofOfPossessionKeyUtils.*;
import static ch.admin.bj.swiyu.core.business.test.TrustOnboardingSubmissionTestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bit.jeap.security.test.WithJeapAuthenticationToken;
import ch.admin.bj.swiyu.antivirus.client.api.ScanApi;
import ch.admin.bj.swiyu.antivirus.client.model.ScanResult;
import ch.admin.bj.swiyu.core.business.common.api.AddressDto;
import ch.admin.bj.swiyu.core.business.common.api.ContactDto;
import ch.admin.bj.swiyu.core.business.common.api.MultiLanguageTextDto;
import ch.admin.bj.swiyu.core.business.common.audit.AuditPublisher;
import ch.admin.bj.swiyu.core.business.common.did.DidPublicKeyLoader;
import ch.admin.bj.swiyu.core.business.common.domain.*;
import ch.admin.bj.swiyu.core.business.common.exceptions.ResourceNotFoundException;
import ch.admin.bj.swiyu.core.business.common.exceptions.ValidationException;
import ch.admin.bj.swiyu.core.business.modules.documents.service.PartnerDocumentService;
import ch.admin.bj.swiyu.core.business.modules.management.api.BusinessPartnerTrustStatusDto;
import ch.admin.bj.swiyu.core.business.modules.management.api.CreateBusinessEntityDto;
import ch.admin.bj.swiyu.core.business.modules.management.domain.pams.PamsClient;
import ch.admin.bj.swiyu.core.business.modules.management.service.BusinessPartnerService;
import ch.admin.bj.swiyu.core.business.modules.trust.api.*;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.*;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.publisher.DomainEventPublisher;
import ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData;
import ch.admin.bj.swiyu.core.business.test.TestRepositories;
import ch.admin.bj.swiyu.core.business.test.TrustOnboardingSubmissionTestData;
import ch.admin.bj.swiyu.core.business.test.container.WithAllTestContainerInitializers;
import ch.admin.eportal.pams.client.api.BusinessPartnerApi;
import com.nimbusds.jose.JOSEException;
import jakarta.persistence.OptimisticLockException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;

@ActiveProfiles("test")
@SpringBootTest
@WithAllTestContainerInitializers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@WithJeapAuthenticationToken(username = "test")
class TrustOnboardingServiceIT {

    @MockitoBean
    ScanApi scanApi;

    @MockitoBean
    DomainEventPublisher domainEventPublisher;

    @MockitoBean
    AuditPublisher auditPublisher;

    @MockitoBean(answers = Answers.RETURNS_DEEP_STUBS)
    @Qualifier("didResolverClient")
    RestClient didResolverClient;

    @MockitoBean(answers = Answers.RETURNS_DEEP_STUBS)
    BusinessPartnerApi businessPartnerApi;

    @MockitoBean
    DidPublicKeyLoader didPublicKeyLoader;

    @MockitoBean
    PamsClient pamsClient;

    @Autowired
    PartnerDocumentService partnerDocumentService;

    @Autowired
    TrustOnboardingService service;

    @Autowired
    TestRepositories repos;

    @Autowired
    BusinessPartnerService businessPartnerService;

    private static String lookupPamsAdminUserUid() {
        return (
            (JeapAuthenticationToken) SecurityContextHolder.getContext().getAuthentication()
        ).getPreferredUsername();
    }

    @BeforeEach
    void setup() {
        repos.truncateTables();
        BusinessEntityTestData.insertTestBusinessPartners(repos.businessPartner);
        when(scanApi.scanGet(any())).thenReturn(
            List.of(
                new ScanResult()
                    .result("OK")
                    .requestID(UUID.randomUUID())
                    .description("description")
                    .clamavVersion("clamav-v1")
                    .clamavDatabaseVersion("clamav-db-v1")
            )
        );
    }

    @Test
    void createTrustOnboardingSubmission_succeeds() {
        var request = trustOnboardingSubmissionRequestDto();
        var resultDto = service.createTrustOnboardingSubmission(request);

        assertNotNull(resultDto);
        assertNotNull(resultDto.id());
        assertEquals(request.partnerId(), resultDto.partnerId());
        assertEquals(request.getEntityName(), resultDto.entityName());
        assertEquals(request.entityEmail(), resultDto.entityEmail());
        assertEquals(request.getContactPerson(), resultDto.contactPerson());
        assertEquals(request.getRegistryIds().get("UID"), resultDto.registryIds().get("UID"));
        assertEquals(request.correspondingLanguage(), resultDto.correspondingLanguage());
        assertEquals(request.dids(), resultDto.proofOfPossessions().stream().map(ProofOfPossessionDto::did).toList());
        assertEquals(TrustOnboardingSubmissionStatus.UNSUBMITTED.name(), resultDto.status().name());

        var persistedEntity = repos.trustOnboardingSubmission.findById(resultDto.id());
        assertTrue(persistedEntity.isPresent());
        var submission = persistedEntity.get();
        assertEquals(request.partnerId(), submission.getPartnerId());
        assertEquals(TrustOnboardingSubmissionStatus.UNSUBMITTED, submission.getStatus());
        assertEquals(request.getEntityName().de(), submission.getEntityName().getDe());
        assertNotNull(submission.getProofOfPossessions());
        assertEquals(2, submission.getProofOfPossessions().size());
        assertEquals(request.dids().getFirst(), submission.getProofOfPossessions().getFirst().getDid());

        // Calling again should return the same submission
        var secondResultDto = service.createTrustOnboardingSubmission(request);
        assertEquals(resultDto.id(), secondResultDto.id());
        assertEquals(1, repos.trustOnboardingSubmission.count());
    }

    @Test
    void createTrustOnboardingSubmissionWhenUnsubmittedSubmissionExists_fails() {
        var initial = repos.trustOnboardingSubmission.saveAndFlush(trustOnboardingSubmission());

        var request = trustOnboardingSubmissionRequestDto();
        // trying to create a new submission while there is still one open returns it instead
        var submission = service.createTrustOnboardingSubmission(request);
        assertEquals(initial.getId(), submission.id());
        assertEquals(1, repos.trustOnboardingSubmission.count());
        var testData = trustOnboardingSubmission();
        assertThrows(DataIntegrityViolationException.class, () ->
            repos.trustOnboardingSubmission.saveAndFlush(testData)
        );
    }

    @Test
    void createTrustOnboardingSubmissionWhenInformationRequestedSubmissionExists_fails() {
        var initial = repos.trustOnboardingSubmission.saveAndFlush(trustOnboardingSubmission());
        initial.markAsInformationRequested(TrustOnboardingDeclineReason.MISSING_DOCUMENTS, "partnerNote");

        assertEquals(1, repos.trustOnboardingSubmission.count());
        var testData = trustOnboardingSubmission();

        assertThrows(DataIntegrityViolationException.class, () ->
            repos.trustOnboardingSubmission.saveAndFlush(testData)
        );
    }

    @Test
    void createTrustOnboardingSubmissionWhenSubmittedSubmissionExists_fails() {
        var initial = repos.trustOnboardingSubmission.saveAndFlush(trustOnboardingSubmission());
        initial.markAsSubmitted();

        assertEquals(1, repos.trustOnboardingSubmission.count());
        var testData = trustOnboardingSubmission();

        assertThrows(DataIntegrityViolationException.class, () ->
            repos.trustOnboardingSubmission.saveAndFlush(testData)
        );
    }

    @Test
    void getTrustOnboardingSubmission_succeeds() {
        var initial = repos.trustOnboardingSubmission.save(trustOnboardingSubmission());

        var submission = service.getTrustOnboardingSubmission(initial.getId());

        assertNotNull(submission);
        assertEquals(BusinessEntityTestData.DEFAULT_ENTITY, submission.partnerId());
    }

    @Test
    void getTrustOnboardingSubmission_fails_when_not_found() {
        var uuid = UUID.randomUUID();
        assertThrows(ResourceNotFoundException.class, () -> service.getTrustOnboardingSubmission(uuid));
    }

    @Test
    void updateTrustOnboardingSubmission_succeeds() {
        var initial = repos.trustOnboardingSubmission.save(trustOnboardingSubmission());
        var updateDto = trustOnboardingSubmissionRequestDtoUpdate();
        var submission = service.updateTrustOnboardingSubmission(initial.getId(), updateDto);

        assertNotNull(submission);
        assertEquals(updateDto.partnerId(), submission.partnerId());
        assertEquals(updateDto.getEntityName(), submission.entityName());
        assertEquals(updateDto.entityAddress(), submission.address());
        assertEquals(updateDto.entityEmail(), submission.entityEmail());
        assertEquals(updateDto.getContactPerson(), submission.contactPerson());
        assertEquals(updateDto.getRegistryIds().get("UID"), submission.registryIds().get("UID"));
        assertEquals(updateDto.correspondingLanguage(), submission.correspondingLanguage());
        assertEquals(
            updateDto.dids(),
            submission.proofOfPossessions().stream().map(ProofOfPossessionDto::did).toList()
        );
        assertEquals(TrustOnboardingSubmissionStatus.UNSUBMITTED.name(), submission.status().name());
    }

    @Test
    void updateTrustOnboardingSubmission_fails_when_not_found() {
        var uuid = UUID.randomUUID();
        var updateDTO = trustOnboardingSubmissionRequestDtoUpdate();
        assertThrows(ResourceNotFoundException.class, () -> service.updateTrustOnboardingSubmission(uuid, updateDTO));
    }

    @Test
    void updateTrustOnboarding_replacesWithSameDidWhenUnsubmitted() {
        // GIVEN
        var request = trustOnboardingSubmissionRequestDto();
        var resultDto = service.createTrustOnboardingSubmission(request);
        var originalPoPs = resultDto.proofOfPossessions();

        // WHEN Resubmitting with identical dids
        TrustOnboardingSubmissionRequestDto requestDto = TrustOnboardingSubmissionRequestDto.builder()
            .partnerId(resultDto.partnerId())
            .entityName(resultDto.entityName())
            .entityEmail(resultDto.entityEmail())
            .entityAddress(resultDto.address())
            .contactPerson(resultDto.contactPerson())
            .correspondingLanguage(resultDto.correspondingLanguage())
            .registryIds(resultDto.registryIds())
            .dids(resultDto.proofOfPossessions().stream().map(ProofOfPossessionDto::did).toList())
            .build();
        var resultDto2 = service.updateTrustOnboardingSubmission(resultDto.id(), requestDto);

        // THEN should keep the dids and nonce values
        resultDto2
            .proofOfPossessions()
            .forEach(pop ->
                Assertions.assertThat(
                    originalPoPs.stream().anyMatch(o -> o.did().equals(pop.did()) && o.nonce().equals(pop.nonce()))
                ).isTrue()
            );
    }

    @Test
    void updateTrustOnboarding_resetWhenRequestingInformation()
        throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, JOSEException {
        // GIVEN submitted
        var request = trustOnboardingSubmissionRequestDto();
        var resultDto = service.createTrustOnboardingSubmission(request);

        // Valid pop
        Assertions.assertThat(resultDto.proofOfPossessions()).hasSize(2);
        var pops = new ArrayList<String>();
        var pop1 = resultDto.proofOfPossessions().getFirst();
        var kp1 = generateKeyPair();
        var signer1 = getSigner(kp1.getPrivate());
        var verifier1 = getVerifier(kp1.getPublic());
        pops.add(getPoPSubmission(pop1.nonce(), pop1.did(), pop1.did() + "#suffix", signer1));
        when(didPublicKeyLoader.loadPublicKey(pop1.did() + "#suffix")).thenReturn(verifier1);

        var pop2 = resultDto.proofOfPossessions().getLast();
        var kp2 = generateKeyPair();
        var signer2 = getSigner(kp2.getPrivate());
        var verifier2 = getVerifier(kp2.getPublic());
        pops.add(getPoPSubmission(pop2.nonce(), pop2.did(), pop2.did() + "#suffix", signer2));
        when(didPublicKeyLoader.loadPublicKey(pop2.did() + "#suffix")).thenReturn(verifier2);

        service.submitProofOfPossessions(resultDto.partnerId(), pops);
        var updated = repos.trustOnboardingSubmission.findById(resultDto.id());
        Assertions.assertThat(updated).isPresent();
        Assertions.assertThat(updated.get().getVersion()).isGreaterThan(resultDto.version());
        Assertions.assertThat(
            updated
                .get()
                .getProofOfPossessions()
                .stream()
                .allMatch(pop -> pop.getStatus().name().equals(ProofOfPossessionStatusDto.VALID.name()))
        ).isTrue();

        // Set DOI before submitting (required for submit)
        var forDoi = repos.trustOnboardingSubmission.findById(resultDto.id()).orElseThrow();
        forDoi.updateDeclarationOfIntent(new DeclarationOfIntent(UUID.randomUUID().toString(), null));
        var savedWithDoi = repos.trustOnboardingSubmission.saveAndFlush(forDoi);

        service.submit(resultDto.id(), new TrustOnboardingSubmitRequestDto(savedWithDoi.getVersion()));
        var submitted = repos.trustOnboardingSubmission.findById(resultDto.id());
        Assertions.assertThat(submitted).isPresent();
        Assertions.assertThat(submitted.get().getStatus()).isEqualTo(TrustOnboardingSubmissionStatus.SUBMITTED);

        // WHEN requesting information
        service.markAsInformationRequested(resultDto.id(), "MISSING_DOCUMENTS", "some reason, dont care");

        // THEN should reset all pops to not supplied and status to information requested
        var infoRequested = repos.trustOnboardingSubmission.findById(resultDto.id());
        Assertions.assertThat(infoRequested).isPresent();
        Assertions.assertThat(infoRequested.get().getStatus()).isEqualTo(
            TrustOnboardingSubmissionStatus.INFORMATION_REQUESTED
        );
        Assertions.assertThat(
            infoRequested
                .get()
                .getProofOfPossessions()
                .stream()
                .allMatch(
                    pop ->
                        pop.getStatus().name().equals(ProofOfPossessionStatusDto.NOT_SUPPLIED.name()) &&
                        pop.getVerifiedAt() == null
                )
        ).isTrue();
    }

    @Test
    void updateTrustOnboarding_replacesWithAdditionalDidWhenUnsubmitted() {
        // GIVEN
        var request = trustOnboardingSubmissionRequestDto();
        var resultDto = service.createTrustOnboardingSubmission(request);
        var originalPoPs = resultDto.proofOfPossessions();

        // WHEN Resubmitting with one additional did
        TrustOnboardingSubmissionRequestDto requestDto = TrustOnboardingSubmissionRequestDto.builder()
            .partnerId(resultDto.partnerId())
            .entityName(resultDto.entityName())
            .entityEmail(resultDto.entityEmail())
            .entityAddress(resultDto.address())
            .contactPerson(resultDto.contactPerson())
            .correspondingLanguage(resultDto.correspondingLanguage())
            .registryIds(resultDto.registryIds())
            .dids(
                Stream.concat(
                    resultDto.proofOfPossessions().stream().map(ProofOfPossessionDto::did),
                    Stream.of("did:example:new")
                ).toList()
            )
            .build();
        var resultDto2 = service.updateTrustOnboardingSubmission(resultDto.id(), requestDto);

        // THEN should keep nonce for existing dids and add a nonce for the new one
        Assertions.assertThat(resultDto2.proofOfPossessions()).hasSize(originalPoPs.size() + 1);
        originalPoPs.forEach(pop ->
            Assertions.assertThat(
                resultDto2
                    .proofOfPossessions()
                    .stream()
                    .anyMatch(o -> o.did().equals(pop.did()) && !o.nonce().equals(pop.nonce()))
            ).isTrue()
        );
    }

    @Test
    void updateTrustOnboardingPop_submitWithValidPops()
        throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, JOSEException {
        // GIVEN
        var request = trustOnboardingSubmissionRequestDto();
        var resultDto = service.createTrustOnboardingSubmission(request);
        var originalPoPs = resultDto.proofOfPossessions();
        var currentTime = Instant.now();

        List<String> popsForSubmission = new ArrayList<>();
        for (ProofOfPossessionDto pop : originalPoPs) {
            var kp = generateKeyPair();
            var signer = getSigner(kp.getPrivate());
            var verifier = getVerifier(kp.getPublic());
            popsForSubmission.add(getPoPSubmission(pop.nonce(), pop.did(), pop.did() + "#suffix", signer));
            when(didPublicKeyLoader.loadPublicKey(pop.did() + "#suffix")).thenReturn(verifier);
        }
        // WHEN Submitting valid pops
        var resultDto2 = service.submitProofOfPossessions(resultDto.partnerId(), popsForSubmission);

        // THEN Should not throw
        Assertions.assertThatNoException();
        // THEN Should change to valid status
        Assertions.assertThat(
            resultDto2
                .proofOfPossessions()
                .stream()
                .allMatch(
                    pop ->
                        ProofOfPossessionStatusDto.VALID.equals(pop.status()) && pop.verifiedAt().isAfter(currentTime)
                )
        ).isTrue();
        // THEN should keep the dids and nonce values
        resultDto2
            .proofOfPossessions()
            .forEach(pop ->
                Assertions.assertThat(
                    originalPoPs.stream().anyMatch(o -> o.did().equals(pop.did()) && o.nonce().equals(pop.nonce()))
                ).isTrue()
            );
    }

    @Test
    void updateTrustOnboardingPop_submitWithPartialValidNonceThrows()
        throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, JOSEException {
        // GIVEN
        var request = trustOnboardingSubmissionRequestDto();
        var resultDto = service.createTrustOnboardingSubmission(request);
        var originalPoPs = resultDto.proofOfPossessions();

        List<String> popsForSubmission = new ArrayList<>();
        Assertions.assertThat(originalPoPs).hasSize(2);
        // Valid pop
        var pop1 = originalPoPs.getFirst();
        var kp1 = generateKeyPair();
        var signer1 = getSigner(kp1.getPrivate());
        var verifier1 = getVerifier(kp1.getPublic());
        popsForSubmission.add(getPoPSubmission(pop1.nonce(), pop1.did(), pop1.did() + "#suffix", signer1));
        when(didPublicKeyLoader.loadPublicKey(pop1.did() + "#suffix")).thenReturn(verifier1);
        // Valid signature but invalid nonce
        var pop2 = originalPoPs.getLast();
        var kp2 = generateKeyPair();
        var signer2 = getSigner(kp2.getPrivate());
        var verifier2 = getVerifier(kp2.getPublic());
        popsForSubmission.add(
            getPoPSubmission(UUID.randomUUID().toString(), pop2.did(), pop2.did() + "#suffix", signer2)
        );
        when(didPublicKeyLoader.loadPublicKey(pop2.did() + "#suffix")).thenReturn(verifier2);

        // WHEN Submitting partial valid pops
        var partnerId = resultDto.partnerId();
        Assertions.assertThatExceptionOfType(ValidationException.class)
            .isThrownBy(() -> service.submitProofOfPossessions(partnerId, popsForSubmission))
            .withMessageContaining("mismatching_nonce");
    }

    @Test
    void getAllTrustOnboardings_ByPartnerId_succeeds() {
        repos.trustOnboardingSubmission.save(trustOnboardingSubmission());
        repos.trustOnboardingSubmission.save(
            trustOnboardingSubmission(UUID.randomUUID(), BusinessEntityTestData.ENTITY_B)
        );
        repos.trustOnboardingSubmission.save(
            trustOnboardingSubmission(UUID.randomUUID(), BusinessEntityTestData.ENTITY_C)
        );

        var submissions = service.getAllTrustOnboardings(
            new TrustOnboardingSubmissionFilterDto(
                List.of(BusinessEntityTestData.DEFAULT_ENTITY, BusinessEntityTestData.ENTITY_B)
            ),
            Pageable.ofSize(10)
        );

        assertNotNull(submissions);
        assertEquals(2, submissions.getTotalElements());
    }

    @Test
    void submit_fails_with_validation_errors() {
        var s = repos.trustOnboardingSubmission.save(
            new TrustOnboardingSubmission(
                BusinessEntityTestData.DEFAULT_ENTITY,
                new MultiLanguageText(),
                TrustOnboardingSubmissionStatus.UNSUBMITTED
            )
        ); // intentionally incomplete
        var id = s.getId();
        var request = new TrustOnboardingSubmitRequestDto(0L);
        var ex = assertThrows(ValidationException.class, () -> service.submit(id, request));
        assertEquals("VALIDATION_ERROR", ex.getCode());
        assertFalse(ex.getViolations().isEmpty());
    }

    @Test
    void submit_fails_when_declarationOfIntent_is_missing() {
        var s = TrustOnboardingSubmissionTestData.trustOnboardingSubmissionEmpty();
        s.update(
            new MultiLanguageText("de", "fr", "it", "en", "rm"),
            new Address("street", "city", "postal", "country", "region"),
            "valid@example.org",
            new Contact(
                "first",
                "last",
                "first.last@example.com",
                "+41 79 123 45 67",
                new Address("street", "city", "postal", "country", "region")
            ),
            Language.DE,
            "uid",
            List.of(new ProofOfPossession("did:example:123", UUID.randomUUID().toString()).toValid()),
            BusinessPartnerType.BUSINESS,
            SigningRule.SINGLE_SIGNATURE,
            List.of(new Signatory("John", "Doe", "+41 79 123 45 67", "john.doe@example.com")),
            false
        );
        // DOI intentionally NOT set
        s = repos.trustOnboardingSubmission.save(s);

        final var id = s.getId();
        final var request = new TrustOnboardingSubmitRequestDto(s.getVersion());
        var ex = assertThrows(ValidationException.class, () -> service.submit(id, request));
        assertEquals("VALIDATION_ERROR", ex.getCode());
        Assertions.assertThat(ex.getViolations()).anyMatch(v -> "declarationOfIntent".equals(v.path()));
        verifyNoInteractions(auditPublisher);
    }

    @Test
    void submit_succeeds_when_valid() {
        var s = TrustOnboardingSubmissionTestData.trustOnboardingSubmissionEmpty();
        // populate required fields
        s.update(
            new MultiLanguageText("de", "fr", "it", "en", "rm"),
            new Address("street", "city", "postal", "country", "region"),
            "valid@example.org",
            new Contact(
                "first",
                "last",
                "first.last@example.com",
                "+41 79 123 45 67",
                new Address("street", "city", "postal", "country", "region")
            ),
            Language.DE,
            "uid",
            List.of(new ProofOfPossession("did:example:123", UUID.randomUUID().toString()).toValid()),
            BusinessPartnerType.BUSINESS,
            SigningRule.SINGLE_SIGNATURE,
            List.of(new Signatory("John", "Doe", "+41 79 123 45 67", "john.doe@example.com")),
            false
        );
        // DOI required for submit
        s.updateDeclarationOfIntent(new DeclarationOfIntent(UUID.randomUUID().toString(), null));
        s = repos.trustOnboardingSubmission.save(s);

        TrustOnboardingSubmission finalS = s;
        var request = new TrustOnboardingSubmitRequestDto(finalS.getVersion());
        assertDoesNotThrow(() -> service.submit(finalS.getId(), request));

        var refreshed = repos.trustOnboardingSubmission.findById(s.getId()).orElseThrow();
        // assert status transitioned to SUBMITTED, submittedAt set, etc.
        assertEquals(TrustOnboardingSubmissionStatus.SUBMITTED, refreshed.getStatus());
    }

    @Test
    void submit_version_mismatch_throws() {
        var s = TrustOnboardingSubmissionTestData.trustOnboardingSubmissionEmpty();
        s = repos.trustOnboardingSubmission.save(s);

        final var id = s.getId();

        var request = new TrustOnboardingSubmitRequestDto(s.getVersion() + 1);
        assertThrows(OptimisticLockException.class, () -> service.submit(id, request));
    }

    @Test
    void submitTrustOnboardingSubmission_fails_when_not_found() {
        var uuid = UUID.randomUUID();
        var request = new TrustOnboardingSubmitRequestDto(1L);

        assertThrows(ResourceNotFoundException.class, () -> service.submit(uuid, request));
    }

    @Test
    void markAsRejected_succeeds() {
        var submission = repos.trustOnboardingSubmission.save(trustOnboardingSubmission());

        service.markAsRejected(submission.getId(), "FRAUDULENT_ACTIVITY");

        var refreshed = repos.trustOnboardingSubmission.findById(submission.getId()).orElseThrow();
        assertEquals(TrustOnboardingSubmissionStatus.REJECTED, refreshed.getStatus());
        assertEquals(TrustOnboardingRejectReason.FRAUDULENT_ACTIVITY, refreshed.getRejectReason());
    }

    @Test
    void markAsRejected_fails_when_not_found() {
        var uuid = UUID.randomUUID();
        assertThrows(ResourceNotFoundException.class, () -> service.markAsRejected(uuid, "should fail"));
    }

    @Test
    void markAsInformationRequested_succeeds() {
        var submission = repos.trustOnboardingSubmission.save(trustOnboardingSubmission());

        service.markAsInformationRequested(submission.getId(), "MISSING_DOCUMENTS", "partnerNote");

        var refreshed = repos.trustOnboardingSubmission.findById(submission.getId()).orElseThrow();
        assertEquals(TrustOnboardingSubmissionStatus.INFORMATION_REQUESTED, refreshed.getStatus());
        assertEquals(TrustOnboardingDeclineReason.MISSING_DOCUMENTS, refreshed.getDeclineReason());
        assertEquals("partnerNote", refreshed.getPartnerNote());
    }

    @Test
    void markAsInformationRequested_fails_when_not_found() {
        var uuid = UUID.randomUUID();
        assertThrows(ResourceNotFoundException.class, () ->
            service.markAsInformationRequested(uuid, "OTHER", "should fail")
        );
    }

    @Test
    void markAsSucceeded_succeeds() {
        // GIVEN
        var createBusinessEntityDto = new CreateBusinessEntityDto("Hallo Welt AG", "hello.world@example.com", null);
        var businessEntity = businessPartnerService.createBusinessPartnerV1(
            createBusinessEntityDto,
            lookupPamsAdminUserUid()
        );
        var submission = repos.trustOnboardingSubmission.save(
            trustOnboardingSubmission(UUID.randomUUID(), businessEntity.id())
        );

        service.markAsSucceeded(submission.getId());

        var refreshed = repos.trustOnboardingSubmission.findById(submission.getId()).orElseThrow();
        assertEquals(TrustOnboardingSubmissionStatus.SUCCEEDED, refreshed.getStatus());
    }

    @Test
    void markAsSucceeded_updatesBusinessPartnerWithSubmissionDetails() {
        // GIVEN
        var createBusinessEntityDto = new CreateBusinessEntityDto("Hallo Welt AG", "hello.world@example.com", null);
        var businessEntity = businessPartnerService.createBusinessPartnerV1(
            createBusinessEntityDto,
            lookupPamsAdminUserUid()
        );
        var partnerId = businessEntity.id();

        var submission = TrustOnboardingSubmissionTestData.trustOnboardingSubmission(UUID.randomUUID(), partnerId);
        var newName = new MultiLanguageText("New Name DE", "New Name FR", "New Name IT", "New Name EN", "New Name RM");
        var newAddress = new Address("New Street", "New City", "1234", "CH", "Region");
        var newEmail = "new@example.com";
        var newUid = "CHE-123.456.789";
        var newPhone = "+41 79 123 45 67";
        var newType = BusinessPartnerType.BUSINESS;

        submission.update(
            newName,
            newAddress,
            newEmail,
            new Contact("First", "Last", "contact@example.com", newPhone, newAddress),
            Language.DE,
            newUid,
            List.of(new ProofOfPossession("did:example:123", UUID.randomUUID().toString()).toValid()),
            newType,
            SigningRule.SINGLE_SIGNATURE,
            List.of(new Signatory("First", "Last", newPhone, "contact@example.com")),
            false
        );
        submission = repos.trustOnboardingSubmission.save(submission);

        // WHEN
        service.markAsSucceeded(submission.getId());

        // THEN
        var updatedPartner = businessPartnerService.getBusinessPartner(partnerId);
        assertEquals("New Name DE", updatedPartner.name());
        assertEquals(newEmail, updatedPartner.contactEmailAddress());
        assertEquals(newUid, updatedPartner.uid());
        assertEquals(newPhone, updatedPartner.contactPhone());
        assertEquals(newAddress.getStreet(), updatedPartner.address().street());
        assertEquals(ch.admin.bj.swiyu.core.business.common.api.BusinessPartnerTypeDto.BUSINESS, updatedPartner.type());
    }

    @Test
    void markAsSucceeded_fails_when_not_found() {
        var uuid = UUID.randomUUID();
        assertThrows(ResourceNotFoundException.class, () -> service.markAsSucceeded(uuid));
    }

    @Test
    void uploadDocument_sanitizesStorageKeyAndDisplayName() {
        // GIVEN
        when(scanApi.scanGet(any())).thenReturn(
            List.of(
                new ScanResult()
                    .clamavDatabaseVersion("test")
                    .clamavVersion("test")
                    .description("test")
                    .filename("test")
                    .result("OK")
                    .requestID(UUID.randomUUID())
            )
        );
        var submission = trustOnboardingSubmission(UUID.randomUUID(), BusinessEntityTestData.ENTITY_A);
        repos.trustOnboardingSubmission.save(submission);
        var file = new MockMultipartFile(
            "file",
            "../../test/Ä report\u0007 (final).pdf",
            "application/pdf",
            "A".getBytes(StandardCharsets.UTF_8)
        );

        // WHEN
        var uploaded = service.uploadTrustOnboardingSubmissionDocument(
            submission.getId(),
            new TrustOnboardingSubmissionDocumentUploadRequestDto(
                TrustOnboardingSubmissionDocumentTypeDto.TRUST_ONBOARDING_OTHER,
                file
            )
        );
        var persistedDocument = repos.partnerDocuments.findById(uploaded.id()).orElseThrow();

        // THEN
        assertThat(uploaded.name()).isEqualTo("A_report_(final).pdf");
        assertThat(persistedDocument.getStorageObjectKey()).endsWith("A_report_(final).pdf");
        assertThat(persistedDocument.getStorageObjectKey()).doesNotContain("../");
        verify(auditPublisher).trustOnboardingDocumentUploaded(
            eq(uploaded.id().toString()),
            anyString(),
            anyString(),
            eq(persistedDocument.getStorageObjectKey())
        );
    }

    @Test
    void deleteDocument_clearsDeclarationOfIntent_whenDeletedDocumentIsTheDoi() {
        // GIVEN
        when(scanApi.scanGet(any())).thenReturn(
            List.of(
                new ScanResult()
                    .clamavDatabaseVersion("test")
                    .clamavVersion("test")
                    .description("test")
                    .filename("test")
                    .result("OK")
                    .requestID(UUID.randomUUID())
            )
        );
        var submission = trustOnboardingSubmission(UUID.randomUUID(), BusinessEntityTestData.ENTITY_A);
        repos.trustOnboardingSubmission.save(submission);
        var doiFile = new MockMultipartFile("file", "doi.pdf", "application/pdf", "A".getBytes(StandardCharsets.UTF_8));
        var uploaded = service.uploadTrustOnboardingSubmissionDocument(
            submission.getId(),
            new TrustOnboardingSubmissionDocumentUploadRequestDto(
                TrustOnboardingSubmissionDocumentTypeDto.TRUST_ONBOARDING_DECLARATION_OF_INTENT,
                doiFile
            )
        );

        // WHEN
        service.deleteTrustOnboardingSubmissionDocument(submission.getId(), uploaded.id());

        // THEN
        var refreshed = repos.trustOnboardingSubmission.findById(submission.getId()).orElseThrow();
        assertNull(refreshed.getDeclarationOfIntent());
    }

    @ParameterizedTest
    @MethodSource(
        "ch.admin.bj.swiyu.core.business.test.TrustOnboardingSubmissionTestData#provideUpdateTrustStatus_aggregation_validation"
    )
    void aggregateTrustVerificationStatus_aggregation_validation(
        BusinessPartnerTrustStatusDto targetStatus,
        List<TrustOnboardingSubmissionStatus> sources
    ) {
        // setup existing trust onboardings as precondition
        for (var source : sources) {
            var tos = new TrustOnboardingSubmission(BusinessEntityTestData.DEFAULT_ENTITY, null, source);
            repos.trustOnboardingSubmission.save(tos);
        }

        // update aggregation
        service.aggregateTrustVerificationStatus(BusinessEntityTestData.DEFAULT_ENTITY);

        // validate target status
        var updatedBusinessPartner = businessPartnerService.getBusinessPartner(BusinessEntityTestData.DEFAULT_ENTITY);
        assertEquals(targetStatus, updatedBusinessPartner.trustVerificationStatus());
    }

    @Test
    void updateTrustOnboardingSubmission_discardsDeclarationOfIntent_whenUidChanges() {
        // GIVEN
        var submission = initSubmissionWithDeclarationOfIntent();
        // Change only the UID
        var updateDto = TrustOnboardingSubmissionRequestDto.builder()
            .partnerId(submission.getPartnerId())
            .entityName(trustOnboardingSubmissionRequestDto().getEntityName())
            .entityAddress(trustOnboardingSubmissionRequestDto().entityAddress())
            .entityEmail(trustOnboardingSubmissionRequestDto().entityEmail())
            .contactPerson(trustOnboardingSubmissionRequestDto().getContactPerson())
            .correspondingLanguage(trustOnboardingSubmissionRequestDto().correspondingLanguage())
            .registryIds(Map.of("UID", "CHE-999.999.999")) // changed UID
            .dids(trustOnboardingSubmissionRequestDto().dids())
            .requestedPartnerType(trustOnboardingSubmissionRequestDto().requestedPartnerType())
            .signingRule(trustOnboardingSubmissionRequestDto().signingRule())
            .signatories(trustOnboardingSubmissionRequestDto().signatories())
            .build();

        // WHEN
        service.updateTrustOnboardingSubmission(submission.getId(), updateDto);

        var refreshed = repos.trustOnboardingSubmission.findById(submission.getId()).orElseThrow();
        assertNull(refreshed.getDeclarationOfIntent());
    }

    @Test
    void updateTrustOnboardingSubmission_discardsDeclarationOfIntent_whenEntityNameChanges() {
        var submission = initSubmissionWithDeclarationOfIntent();

        var updateDto = TrustOnboardingSubmissionRequestDto.builder()
            .partnerId(submission.getPartnerId())
            .entityName(new MultiLanguageTextDto("CHANGED", null, null, null, null)) // changed name
            .entityAddress(trustOnboardingSubmissionRequestDto().entityAddress())
            .entityEmail(trustOnboardingSubmissionRequestDto().entityEmail())
            .contactPerson(trustOnboardingSubmissionRequestDto().getContactPerson())
            .correspondingLanguage(trustOnboardingSubmissionRequestDto().correspondingLanguage())
            .registryIds(trustOnboardingSubmissionRequestDto().getRegistryIds())
            .dids(trustOnboardingSubmissionRequestDto().dids())
            .requestedPartnerType(trustOnboardingSubmissionRequestDto().requestedPartnerType())
            .signingRule(trustOnboardingSubmissionRequestDto().signingRule())
            .signatories(trustOnboardingSubmissionRequestDto().signatories())
            .build();

        service.updateTrustOnboardingSubmission(submission.getId(), updateDto);

        var refreshed = repos.trustOnboardingSubmission.findById(submission.getId()).orElseThrow();
        assertNull(refreshed.getDeclarationOfIntent());
    }

    @Test
    void updateTrustOnboardingSubmission_discardsDeclarationOfIntent_whenAddressChanges() {
        var submission = initSubmissionWithDeclarationOfIntent();

        var updateDto = TrustOnboardingSubmissionRequestDto.builder()
            .partnerId(submission.getPartnerId())
            .entityName(trustOnboardingSubmissionRequestDto().getEntityName())
            .entityAddress(
                AddressDto.builder().street("New Street").postalCode("9999").city("New City").country("DE").build()
            ) // changed address
            .entityEmail(trustOnboardingSubmissionRequestDto().entityEmail())
            .contactPerson(trustOnboardingSubmissionRequestDto().getContactPerson())
            .correspondingLanguage(trustOnboardingSubmissionRequestDto().correspondingLanguage())
            .registryIds(trustOnboardingSubmissionRequestDto().getRegistryIds())
            .dids(trustOnboardingSubmissionRequestDto().dids())
            .requestedPartnerType(trustOnboardingSubmissionRequestDto().requestedPartnerType())
            .signingRule(trustOnboardingSubmissionRequestDto().signingRule())
            .signatories(trustOnboardingSubmissionRequestDto().signatories())
            .build();

        service.updateTrustOnboardingSubmission(submission.getId(), updateDto);

        var refreshed = repos.trustOnboardingSubmission.findById(submission.getId()).orElseThrow();
        assertNull(refreshed.getDeclarationOfIntent());
    }

    @Test
    void updateTrustOnboardingSubmission_discardsDeclarationOfIntent_whenDidsChange() {
        var submission = initSubmissionWithDeclarationOfIntent();

        var updateDto = TrustOnboardingSubmissionRequestDto.builder()
            .partnerId(submission.getPartnerId())
            .entityName(trustOnboardingSubmissionRequestDto().getEntityName())
            .entityAddress(trustOnboardingSubmissionRequestDto().entityAddress())
            .entityEmail(trustOnboardingSubmissionRequestDto().entityEmail())
            .contactPerson(trustOnboardingSubmissionRequestDto().getContactPerson())
            .correspondingLanguage(trustOnboardingSubmissionRequestDto().correspondingLanguage())
            .registryIds(trustOnboardingSubmissionRequestDto().getRegistryIds())
            .dids(List.of("did:example:brand-new")) // changed DIDs
            .requestedPartnerType(trustOnboardingSubmissionRequestDto().requestedPartnerType())
            .signingRule(trustOnboardingSubmissionRequestDto().signingRule())
            .signatories(trustOnboardingSubmissionRequestDto().signatories())
            .build();

        service.updateTrustOnboardingSubmission(submission.getId(), updateDto);

        var refreshed = repos.trustOnboardingSubmission.findById(submission.getId()).orElseThrow();
        assertNull(refreshed.getDeclarationOfIntent());
    }

    @Test
    void updateTrustOnboardingSubmission_discardsDeclarationOfIntent_whenSigningRuleChanges() {
        var submission = initSubmissionWithDeclarationOfIntent();

        // SINGLE_SIGNATURE → JOINT_SIGNATURE_TWO requires 2 signatories
        var updateDto = TrustOnboardingSubmissionRequestDto.builder()
            .partnerId(submission.getPartnerId())
            .entityName(trustOnboardingSubmissionRequestDto().getEntityName())
            .entityAddress(trustOnboardingSubmissionRequestDto().entityAddress())
            .entityEmail(trustOnboardingSubmissionRequestDto().entityEmail())
            .contactPerson(trustOnboardingSubmissionRequestDto().getContactPerson())
            .correspondingLanguage(trustOnboardingSubmissionRequestDto().correspondingLanguage())
            .registryIds(trustOnboardingSubmissionRequestDto().getRegistryIds())
            .dids(trustOnboardingSubmissionRequestDto().dids())
            .requestedPartnerType(trustOnboardingSubmissionRequestDto().requestedPartnerType())
            .signingRule(ch.admin.bj.swiyu.core.business.modules.trust.api.SigningRuleDto.JOINT_SIGNATURE_TWO) // changed signing rule
            .signatories(
                List.of(
                    new SignatoryDto("John", "Doe", "+41 79 123 45 67", "john@example.com"),
                    new SignatoryDto("Jane", "Doe", "+41 79 123 45 68", "jane@example.com")
                )
            )
            .build();

        service.updateTrustOnboardingSubmission(submission.getId(), updateDto);

        var refreshed = repos.trustOnboardingSubmission.findById(submission.getId()).orElseThrow();
        assertNull(refreshed.getDeclarationOfIntent());
    }

    @Test
    void updateTrustOnboardingSubmission_discardsDeclarationOfIntent_whenSignatoriesChange() {
        var submission = initSubmissionWithDeclarationOfIntent();

        var updateDto = TrustOnboardingSubmissionRequestDto.builder()
            .partnerId(submission.getPartnerId())
            .entityName(trustOnboardingSubmissionRequestDto().getEntityName())
            .entityAddress(trustOnboardingSubmissionRequestDto().entityAddress())
            .entityEmail(trustOnboardingSubmissionRequestDto().entityEmail())
            .contactPerson(trustOnboardingSubmissionRequestDto().getContactPerson())
            .correspondingLanguage(trustOnboardingSubmissionRequestDto().correspondingLanguage())
            .registryIds(trustOnboardingSubmissionRequestDto().getRegistryIds())
            .dids(trustOnboardingSubmissionRequestDto().dids())
            .requestedPartnerType(trustOnboardingSubmissionRequestDto().requestedPartnerType())
            .signingRule(trustOnboardingSubmissionRequestDto().signingRule())
            .signatories(
                List.of(
                    new SignatoryDto("Jane", "Smith", "+41 79 999 99 99", "jane.smith@example.com") // changed signatory
                )
            )
            .build();

        service.updateTrustOnboardingSubmission(submission.getId(), updateDto);

        var refreshed = repos.trustOnboardingSubmission.findById(submission.getId()).orElseThrow();
        assertNull(refreshed.getDeclarationOfIntent());
    }

    @Test
    void updateTrustOnboardingSubmission_keepsDeclarationOfIntent_whenOnlyNonRelevantFieldsChange() {
        var submission = initSubmissionWithDeclarationOfIntent();

        // Change only email and contact person (not DOI-relevant)
        var baseDto = trustOnboardingSubmissionRequestDto();
        var updateDto = TrustOnboardingSubmissionRequestDto.builder()
            .partnerId(submission.getPartnerId())
            .entityName(baseDto.getEntityName()) // unchanged
            .entityAddress(baseDto.entityAddress()) // unchanged
            .entityEmail("changed@email.com") // changed – not DOI-relevant
            .contactPerson(
                ContactDto.builder()
                    .firstName("New")
                    .lastName("Contact")
                    .email("new.contact@example.com")
                    .phone("+41 79 000 00 00")
                    .build()
            ) // changed – not DOI-relevant
            .correspondingLanguage(baseDto.correspondingLanguage())
            .registryIds(baseDto.getRegistryIds()) // unchanged UID
            .dids(baseDto.dids()) // unchanged
            .requestedPartnerType(baseDto.requestedPartnerType())
            .signingRule(baseDto.signingRule()) // unchanged
            .signatories(baseDto.signatories()) // unchanged
            .build();

        service.updateTrustOnboardingSubmission(submission.getId(), updateDto);

        var refreshed = repos.trustOnboardingSubmission.findById(submission.getId()).orElseThrow();
        assertNotNull(refreshed.getDeclarationOfIntent());
    }

    private @NonNull TrustOnboardingSubmission initSubmissionWithDeclarationOfIntent() {
        var submission = repos.trustOnboardingSubmission.save(trustOnboardingSubmission());
        var document = partnerDocumentService.createTrustOnboardingSubmissionDocument(
            submission.getPartnerId(),
            submission.getId(),
            TRUST_ONBOARDING_DECLARATION_OF_INTENT,
            mockMultipartFile()
        );
        submission.updateDeclarationOfIntent(new DeclarationOfIntent(document.id().toString(), null));
        repos.trustOnboardingSubmission.save(submission);
        return submission;
    }

    private static @NonNull MockMultipartFile mockMultipartFile() {
        return new MockMultipartFile(
            "file",
            "doi.pdf",
            "application/pdf",
            "test-content".getBytes(StandardCharsets.UTF_8)
        );
    }
}
