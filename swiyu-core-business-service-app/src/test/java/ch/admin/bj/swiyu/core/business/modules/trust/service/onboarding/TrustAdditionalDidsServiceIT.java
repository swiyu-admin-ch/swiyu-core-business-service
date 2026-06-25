package ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding;

import static ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding.ProofOfPossessionKeyUtils.generateKeyPair;
import static ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding.ProofOfPossessionKeyUtils.getPoPSubmission;
import static ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding.ProofOfPossessionKeyUtils.getSigner;
import static ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding.ProofOfPossessionKeyUtils.getVerifier;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import ch.admin.bit.jeap.security.test.WithJeapAuthenticationToken;
import ch.admin.bj.swiyu.core.business.common.did.DidPublicKeyLoader;
import ch.admin.bj.swiyu.core.business.common.exceptions.ResourceNotFoundException;
import ch.admin.bj.swiyu.core.business.common.exceptions.ValidationException;
import ch.admin.bj.swiyu.core.business.modules.identifier.domain.IdentifierEntry;
import ch.admin.bj.swiyu.core.business.modules.management.domain.pams.PamsClient;
import ch.admin.bj.swiyu.core.business.modules.trust.api.TrustAdditionalDidsSubmissionCreateRequestDto;
import ch.admin.bj.swiyu.core.business.modules.trust.api.TrustAdditionalDidsSubmissionUpdateRequestDto;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.ProofOfPossession;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.ProofOfPossessionStatus;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustAdditionalDidsRejectReason;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustAdditionalDidsSubmission;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustAdditionalDidsSubmissionRepository;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustAdditionalDidsSubmissionStatus;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.publisher.DomainEventPublisher;
import ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData;
import ch.admin.bj.swiyu.core.business.test.TestRepositories;
import ch.admin.bj.swiyu.core.business.test.container.WithAllTestContainerInitializers;
import ch.admin.bj.swiyu.messagetype.ti.RejectReason;
import ch.admin.bj.swiyu.trust.registry.client.api.TrustStatementApi;
import ch.admin.eportal.pams.client.api.BusinessPartnerApi;
import com.nimbusds.jose.JOSEException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;

@ActiveProfiles("test")
@SpringBootTest
@WithAllTestContainerInitializers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@WithJeapAuthenticationToken(username = "test")
class TrustAdditionalDidsServiceIT {

    @MockitoBean
    DomainEventPublisher domainEventPublisher;

    @MockitoBean(answers = Answers.RETURNS_DEEP_STUBS)
    @Qualifier("didResolverClient")
    RestClient didResolverClient;

    @MockitoBean(answers = Answers.RETURNS_DEEP_STUBS)
    BusinessPartnerApi businessPartnerApi;

    @MockitoBean
    DidPublicKeyLoader didPublicKeyLoader;

    @MockitoBean
    PamsClient pamsClient;

    @MockitoBean
    TrustStatementApi trustStatementApi;

    @Autowired
    TrustAdditionalDidsService service;

    @Autowired
    TrustAdditionalDidsSubmissionRepository repository;

    @Autowired
    TestRepositories testRepositories;

    private final UUID partnerId = BusinessEntityTestData.DEFAULT_ENTITY;
    private final String permissionDid = "did:tdw:example.com:permission";
    private final String didToAdd1 = "did:tdw:example.com:add1";

    @BeforeEach
    void setUp() {
        testRepositories.truncateTables();
        BusinessEntityTestData.insertTestBusinessPartners(testRepositories.businessPartner);

        var permissionEntry = new IdentifierEntry(UUID.randomUUID(), partnerId);
        permissionEntry.updateDidAndActivate(permissionDid);
        testRepositories.identifierEntry.save(permissionEntry);

        var didToAddEntry = new IdentifierEntry(UUID.randomUUID(), partnerId);
        didToAddEntry.updateDidAndActivate(didToAdd1);
        testRepositories.identifierEntry.save(didToAddEntry);
    }

    @Test
    void createSubmission_persistsToDbWithSharedNonce() {
        when(trustStatementApi.getIdentityTrustStatementsForDid(permissionDid, true)).thenReturn(
            List.of("some-trust-statement")
        );

        var result = service.createSubmission(
            partnerId,
            new TrustAdditionalDidsSubmissionCreateRequestDto(permissionDid, List.of(didToAdd1))
        );

        assertThat(result).isNotNull();
        assertThat(result.id()).isNotNull();

        var persisted = repository.findById(result.id());
        assertThat(persisted).isPresent();
        var submission = persisted.get();
        assertThat(submission.getPartnerId()).isEqualTo(partnerId);
        assertThat(submission.getStatus()).isEqualTo(TrustAdditionalDidsSubmissionStatus.UNSUBMITTED);
        assertThat(submission.getPermissionDid().getDid()).isEqualTo(permissionDid);
        assertThat(submission.getPermissionDid().getStatus()).isEqualTo(ProofOfPossessionStatus.NOT_SUPPLIED);
        assertThat(submission.getPermissionDid().getVerifiedAt()).isNull();
        assertThat(submission.getDidsToAdd()).hasSize(1);
        assertThat(submission.getDidsToAdd().getFirst().getDid()).isEqualTo(didToAdd1);
        assertThat(submission.getDidsToAdd().getFirst().getStatus()).isEqualTo(ProofOfPossessionStatus.NOT_SUPPLIED);
        assertThat(submission.getDidsToAdd().getFirst().getVerifiedAt()).isNull();
        assertThat(submission.getPermissionDid().getNonce()).isEqualTo(submission.getDidsToAdd().getFirst().getNonce());
    }

    @Test
    void createSubmission_permissionDidNotBelongingToPartner_throwsAndDoesNotPersist() {
        var dto = new TrustAdditionalDidsSubmissionCreateRequestDto("did:tdw:example.com:unknown", List.of(didToAdd1));

        assertThatThrownBy(() -> service.createSubmission(partnerId, dto)).isInstanceOf(ValidationException.class);
        assertThat(repository.count()).isZero();
    }

    @Test
    void createSubmission_permissionDidHasNoTrustStatement_throwsAndDoesNotPersist() {
        when(trustStatementApi.getIdentityTrustStatementsForDid(permissionDid, true)).thenReturn(List.of());
        var dto = new TrustAdditionalDidsSubmissionCreateRequestDto(permissionDid, List.of(didToAdd1));

        assertThatThrownBy(() -> service.createSubmission(partnerId, dto)).isInstanceOf(ValidationException.class);
        assertThat(repository.count()).isZero();
    }

    @Test
    void createSubmission_didToAddNotBelongingToPartner_throwsAndDoesNotPersist() {
        when(trustStatementApi.getIdentityTrustStatementsForDid(permissionDid, true)).thenReturn(
            List.of("some-trust-statement")
        );
        var dto = new TrustAdditionalDidsSubmissionCreateRequestDto(
            permissionDid,
            List.of("did:tdw:example.com:unknown")
        );

        assertThatThrownBy(() -> service.createSubmission(partnerId, dto)).isInstanceOf(ValidationException.class);
        assertThat(repository.count()).isZero();
    }

    @Test
    void submitWithProofsOfPossession_invalidJwts_refreshesNonceInDb()
        throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, JOSEException {
        var sharedNonce = UUID.randomUUID().toString();
        var kpPermission = generateKeyPair();
        var kpWrong = generateKeyPair();
        var permissionKeyId = permissionDid + "#key1";

        var submission = repository.save(
            new TrustAdditionalDidsSubmission(
                partnerId,
                new ProofOfPossession(permissionDid, sharedNonce),
                List.of(new ProofOfPossession(didToAdd1, sharedNonce))
            )
        );
        var submissionId = submission.getId();

        when(didPublicKeyLoader.loadPublicKey(permissionKeyId)).thenReturn(getVerifier(kpWrong.getPublic()));
        var jwt = getPoPSubmission(sharedNonce, permissionDid, permissionKeyId, getSigner(kpPermission.getPrivate()));
        var dto = new TrustAdditionalDidsSubmissionUpdateRequestDto(List.of(jwt));

        assertThatThrownBy(() -> service.submitWithProofsOfPossession(submissionId, partnerId, dto)).isInstanceOf(
            ValidationException.class
        );

        var refreshed = repository.findById(submissionId).orElseThrow();
        assertThat(refreshed.getPermissionDid().getNonce()).isNotEqualTo(sharedNonce);
        assertThat(refreshed.getStatus()).isEqualTo(TrustAdditionalDidsSubmissionStatus.UNSUBMITTED);
        assertThat(refreshed.getPermissionDid().getNonce()).isEqualTo(refreshed.getDidsToAdd().getFirst().getNonce());
    }

    @Test
    void submitWithProofsOfPossession_submissionNotInUnsubmittedState_throwsAndDoesNotChangeDb() {
        var sharedNonce = UUID.randomUUID().toString();
        var submission = new TrustAdditionalDidsSubmission(
            partnerId,
            new ProofOfPossession(permissionDid, sharedNonce),
            List.of(new ProofOfPossession(didToAdd1, sharedNonce))
        );
        submission.markAsValidatedAndSubmitted();
        var saved = repository.save(submission);
        var submissionId = saved.getId();

        var dto = new TrustAdditionalDidsSubmissionUpdateRequestDto(List.of("some-jwt"));

        assertThatThrownBy(() -> service.submitWithProofsOfPossession(submissionId, partnerId, dto)).isInstanceOf(
            ValidationException.class
        );

        var unchanged = repository.findById(submissionId).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(TrustAdditionalDidsSubmissionStatus.SUBMITTED);
    }

    @Test
    void submitWithProofsOfPossession_validJwts_persistsValidStatusAndVerifiedAt()
        throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, JOSEException {
        var sharedNonce = UUID.randomUUID().toString();
        var kpPermission = generateKeyPair();
        var kpAdd = generateKeyPair();
        var permissionKeyId = permissionDid + "#key1";
        var addKeyId = didToAdd1 + "#key1";

        var submission = repository.save(
            new TrustAdditionalDidsSubmission(
                partnerId,
                new ProofOfPossession(permissionDid, sharedNonce),
                List.of(new ProofOfPossession(didToAdd1, sharedNonce))
            )
        );
        var submissionId = submission.getId();
        var beforeSubmit = Instant.now();

        when(didPublicKeyLoader.loadPublicKey(permissionKeyId)).thenReturn(getVerifier(kpPermission.getPublic()));
        when(didPublicKeyLoader.loadPublicKey(addKeyId)).thenReturn(getVerifier(kpAdd.getPublic()));
        var jwtPermission = getPoPSubmission(
            sharedNonce,
            permissionDid,
            permissionKeyId,
            getSigner(kpPermission.getPrivate())
        );
        var jwtAdd = getPoPSubmission(sharedNonce, didToAdd1, addKeyId, getSigner(kpAdd.getPrivate()));
        var dto = new TrustAdditionalDidsSubmissionUpdateRequestDto(List.of(jwtPermission, jwtAdd));

        service.submitWithProofsOfPossession(submissionId, partnerId, dto);

        var persisted = repository.findById(submissionId).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(TrustAdditionalDidsSubmissionStatus.SUBMITTED);
        assertThat(persisted.getPermissionDid().getStatus()).isEqualTo(ProofOfPossessionStatus.VALID);
        assertThat(persisted.getPermissionDid().getVerifiedAt()).isAfter(beforeSubmit);
        assertThat(persisted.getDidsToAdd().getFirst().getStatus()).isEqualTo(ProofOfPossessionStatus.VALID);
        assertThat(persisted.getDidsToAdd().getFirst().getVerifiedAt()).isAfter(beforeSubmit);
    }

    @Test
    void markAsSucceeded_existingSubmission_updatesStatusToSucceeded() {
        var sharedNonce = UUID.randomUUID().toString();
        var submission = repository.save(
            new TrustAdditionalDidsSubmission(
                partnerId,
                new ProofOfPossession(permissionDid, sharedNonce),
                List.of(new ProofOfPossession(didToAdd1, sharedNonce))
            )
        );
        var submissionId = submission.getId();

        service.markAsSucceeded(submissionId);

        var persisted = repository.findById(submissionId).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(TrustAdditionalDidsSubmissionStatus.SUCCEEDED);
    }

    @Test
    void markAsSucceeded_nonExistentSubmission_throwsResourceNotFoundException() {
        var nonExistentId = UUID.randomUUID();

        assertThatThrownBy(() -> service.markAsSucceeded(nonExistentId)).isInstanceOf(ResourceNotFoundException.class);
    }

    @ParameterizedTest
    @NullSource
    @EnumSource(RejectReason.class)
    void markAsRejected_anyRejectReason_updatesStatusAndDefaultsToUnknown(RejectReason rejectReason) {
        var sharedNonce = UUID.randomUUID().toString();
        var submission = repository.save(
            new TrustAdditionalDidsSubmission(
                partnerId,
                new ProofOfPossession(permissionDid, sharedNonce),
                List.of(new ProofOfPossession(didToAdd1, sharedNonce))
            )
        );
        var submissionId = submission.getId();

        service.markAsRejected(submissionId, rejectReason);

        var persisted = repository.findById(submissionId).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(TrustAdditionalDidsSubmissionStatus.REJECTED);
        assertThat(persisted.getRejectReason()).isEqualTo(TrustAdditionalDidsRejectReason.UNKNOWN);
    }

    @Test
    void markAsRejected_nonExistentSubmission_throwsResourceNotFoundException() {
        var nonExistentId = UUID.randomUUID();

        assertThatThrownBy(() -> service.markAsRejected(nonExistentId, RejectReason.UNKNOWN)).isInstanceOf(
            ResourceNotFoundException.class
        );
    }
}
