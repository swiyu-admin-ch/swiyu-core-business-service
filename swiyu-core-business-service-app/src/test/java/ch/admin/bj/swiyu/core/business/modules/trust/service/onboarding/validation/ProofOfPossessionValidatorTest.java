package ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding.validation;

import static ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding.ProofOfPossessionKeyUtils.*;
import static ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding.ProofOfPossessionKeyUtils.getPoPSubmission;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.admin.bj.swiyu.core.business.common.did.CryptoIntegrityValidator;
import ch.admin.bj.swiyu.core.business.common.did.DidPublicKeyLoader;
import ch.admin.bj.swiyu.core.business.modules.trust.config.TrustOnboardingSubmissionProofOfPossessionProperties;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.ProofOfPossession;
import com.nimbusds.jose.JOSEException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProofOfPossessionValidatorTest {

    DidPublicKeyLoader didPublicKeyLoader;

    ProofOfPossessionValidator proofOfPossessionValidator;

    @BeforeEach
    void beforeEach() {
        didPublicKeyLoader = mock(DidPublicKeyLoader.class);
        var properties = new TrustOnboardingSubmissionProofOfPossessionProperties(Duration.parse("P1D"));
        var cryptoValidator = new CryptoIntegrityValidator(didPublicKeyLoader);
        proofOfPossessionValidator = new ProofOfPossessionValidator(cryptoValidator, properties);
    }

    @Test
    void isDidSelectionEqual_withMatchingDids_shouldReturnTrue() {
        var pop1 = new ProofOfPossession("did:example:123", UUID.randomUUID().toString());
        var pop2 = new ProofOfPossession("did:example:456", UUID.randomUUID().toString());
        var currentPops = List.of(pop1, pop2);
        var targetDids = List.of("did:example:123", "did:example:456");

        var result = proofOfPossessionValidator.isDidSelectionEqual(currentPops, targetDids);

        Assertions.assertThat(result).isTrue();
    }

    @Test
    void isDidSelectionEqual_withMatchingDidsInDifferentOrder_shouldReturnTrue() {
        var pop1 = new ProofOfPossession("did:example:123", UUID.randomUUID().toString());
        var pop2 = new ProofOfPossession("did:example:456", UUID.randomUUID().toString());
        var pop3 = new ProofOfPossession("did:example:789", UUID.randomUUID().toString());
        var currentPops = List.of(pop1, pop2, pop3);
        var targetDids = List.of("did:example:789", "did:example:123", "did:example:456");

        var result = proofOfPossessionValidator.isDidSelectionEqual(currentPops, targetDids);

        Assertions.assertThat(result).isTrue();
    }

    @Test
    void isDidSelectionEqual_withDifferentDids_shouldReturnFalse() {
        var pop1 = new ProofOfPossession("did:example:123", UUID.randomUUID().toString());
        var pop2 = new ProofOfPossession("did:example:456", UUID.randomUUID().toString());
        var currentPops = List.of(pop1, pop2);
        var targetDids = List.of("did:example:123", "did:example:999");

        var result = proofOfPossessionValidator.isDidSelectionEqual(currentPops, targetDids);

        Assertions.assertThat(result).isFalse();
    }

    @Test
    void isDidSelectionEqual_withMoreTargetDids_shouldReturnFalse() {
        var pop1 = new ProofOfPossession("did:example:123", UUID.randomUUID().toString());
        var currentPops = List.of(pop1);
        var targetDids = List.of("did:example:123", "did:example:456");

        var result = proofOfPossessionValidator.isDidSelectionEqual(currentPops, targetDids);

        Assertions.assertThat(result).isFalse();
    }

    @Test
    void isDidSelectionEqual_withFewerTargetDids_shouldReturnFalse() {
        var pop1 = new ProofOfPossession("did:example:123", UUID.randomUUID().toString());
        var pop2 = new ProofOfPossession("did:example:456", UUID.randomUUID().toString());
        var currentPops = List.of(pop1, pop2);
        var targetDids = List.of("did:example:123");

        var result = proofOfPossessionValidator.isDidSelectionEqual(currentPops, targetDids);

        Assertions.assertThat(result).isFalse();
    }

    @Test
    void isDidSelectionEqual_withEmptyLists_shouldReturnTrue() {
        var currentPops = List.<ProofOfPossession>of();
        var targetDids = List.<String>of();

        var result = proofOfPossessionValidator.isDidSelectionEqual(currentPops, targetDids);

        Assertions.assertThat(result).isTrue();
    }

    @Test
    void isDidSelectionEqual_withNullTargetDids_shouldReturnTrueIfCurrentIsEmpty() {
        var currentPops = List.<ProofOfPossession>of();

        var result = proofOfPossessionValidator.isDidSelectionEqual(currentPops, null);

        Assertions.assertThat(result).isTrue();
    }

    @Test
    void isDidSelectionEqual_withNullTargetDidsAndNonEmptyCurrent_shouldReturnFalse() {
        var pop1 = new ProofOfPossession("did:example:123", UUID.randomUUID().toString());
        var currentPops = List.of(pop1);

        var result = proofOfPossessionValidator.isDidSelectionEqual(currentPops, null);

        Assertions.assertThat(result).isFalse();
    }

    @Test
    void isDidSelectionEqual_withSingleMatchingDid_shouldReturnTrue() {
        var pop1 = new ProofOfPossession("did:example:123", UUID.randomUUID().toString());
        var currentPops = List.of(pop1);
        var targetDids = List.of("did:example:123");

        var result = proofOfPossessionValidator.isDidSelectionEqual(currentPops, targetDids);

        Assertions.assertThat(result).isTrue();
    }

    @Test
    void isDidSelectionEqual_withDuplicateDidsInTarget_shouldReturnFalse() {
        var pop1 = new ProofOfPossession("did:example:123", UUID.randomUUID().toString());
        var currentPops = List.of(pop1);
        var targetDids = List.of("did:example:123", "did:example:123");

        var result = proofOfPossessionValidator.isDidSelectionEqual(currentPops, targetDids);

        Assertions.assertThat(result).isFalse();
    }

    @Test
    void getValidatedProofs_withValidPop()
        throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, JOSEException {
        var did = "did:example:123";
        var didWithFragment = did + "#key1";
        var pop = new ProofOfPossession(did, UUID.randomUUID().toString());
        var kp1 = generateKeyPair();
        var signer1 = getSigner(kp1.getPrivate());
        var popStringList = Stream.of(getPoPSubmission(pop.getNonce(), did, didWithFragment, signer1)).toList();
        when(didPublicKeyLoader.loadPublicKey(didWithFragment)).thenReturn(getVerifier(kp1.getPublic()));
        var pops = Stream.of(pop).toList();
        var errors = proofOfPossessionValidator.validateProofOfPossessionSubmissions(popStringList, pops);
        Assertions.assertThat(errors.hasErrors()).isFalse();
    }

    @Test
    void getValidatedProofs_withMismatchingPrivateKey()
        throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, JOSEException {
        var did = "did:example:123";
        var didWithFragment = did + "#key1";
        var pop = new ProofOfPossession(did, UUID.randomUUID().toString());
        var kp1 = generateKeyPair();
        var kp2 = generateKeyPair();
        var signer1 = getSigner(kp1.getPrivate());
        var popStringList = Stream.of(getPoPSubmission(pop.getNonce(), did, didWithFragment, signer1)).toList();
        when(didPublicKeyLoader.loadPublicKey(didWithFragment)).thenReturn(getVerifier(kp2.getPublic()));
        var pops = Stream.of(pop).toList();
        var errors = proofOfPossessionValidator.validateProofOfPossessionSubmissions(popStringList, pops);
        var objectError = errors
            .getAllErrors()
            .stream()
            .filter(f -> "invalid_crypto_integrity".equals(f.getCode()))
            .findFirst();
        Assertions.assertThat(objectError).isPresent();
        Assertions.assertThat(objectError.get().getDefaultMessage()).contains("Public key verification failed");
    }

    @Test
    void getValidatedProofs_withJwtInvalidPop()
        throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, JOSEException {
        var did = "did:example:123";
        var pop = new ProofOfPossession(did, UUID.randomUUID().toString());
        var kp1 = generateKeyPair();
        var signer1 = getSigner(kp1.getPrivate());
        var popStringList = Stream.of("somethinginvalid" + getPoPSubmission(pop.getNonce(), did, signer1)).toList();
        when(didPublicKeyLoader.loadPublicKey(did)).thenReturn(getVerifier(kp1.getPublic()));
        var pops = Stream.of(pop).toList();
        var errors = proofOfPossessionValidator.validateProofOfPossessionSubmissions(popStringList, pops);
        var objectError = errors
            .getAllErrors()
            .stream()
            .filter(f -> "invalid_jwt".equals(f.getCode()))
            .findFirst();
        Assertions.assertThat(objectError).isPresent();
        Assertions.assertThat(objectError.get().getDefaultMessage()).contains(
            "Couldn't parse provided proof of possession"
        );
    }

    @Test
    void getValidatedProofs_withJwtWithMissingNonce()
        throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, JOSEException {
        var did = "did:example:123";
        var pop = new ProofOfPossession(did, UUID.randomUUID().toString());
        var kp1 = generateKeyPair();
        var signer1 = getSigner(kp1.getPrivate());
        var popStringList = Stream.of(getPoPSubmission("", did, signer1)).toList();
        when(didPublicKeyLoader.loadPublicKey(did)).thenReturn(getVerifier(kp1.getPublic()));
        var pops = Stream.of(pop).toList();
        var errors = proofOfPossessionValidator.validateProofOfPossessionSubmissions(popStringList, pops);
        var objectError = errors
            .getAllErrors()
            .stream()
            .filter(f -> "missing_nonce".equals(f.getCode()))
            .findFirst();
        Assertions.assertThat(objectError).isPresent();
        Assertions.assertThat(objectError.get().getDefaultMessage()).contains(
            "Nonce is missing in the proof of possession"
        );
    }

    @Test
    void getValidatedProofs_withJwtWithMismatchingNonce()
        throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, JOSEException {
        var did = "did:example:123";
        var didWithFragment = did + "#key1";
        var pop = new ProofOfPossession(did, UUID.randomUUID().toString());
        var kp1 = generateKeyPair();
        var signer1 = getSigner(kp1.getPrivate());
        var popStringList = Stream.of(
            getPoPSubmission(UUID.randomUUID().toString(), did, didWithFragment, signer1)
        ).toList();
        when(didPublicKeyLoader.loadPublicKey(didWithFragment)).thenReturn(getVerifier(kp1.getPublic()));
        var pops = Stream.of(pop).toList();
        var errors = proofOfPossessionValidator.validateProofOfPossessionSubmissions(popStringList, pops);
        var objectError = errors
            .getAllErrors()
            .stream()
            .filter(f -> "mismatching_nonce".equals(f.getCode()))
            .findFirst();
        Assertions.assertThat(objectError).isPresent();
        Assertions.assertThat(objectError.get().getDefaultMessage()).contains(
            "Mismatching nonce in proof of possession for DID"
        );
    }

    @Test
    void getValidatedProofs_withJwtWithInvalidNonceFormat()
        throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, JOSEException {
        var did = "did:example:123";
        var pop = new ProofOfPossession(did, UUID.randomUUID().toString());
        var kp1 = generateKeyPair();
        var signer1 = getSigner(kp1.getPrivate());
        var popStringList = Stream.of(getPoPSubmission("definitely-not-an-uuid", did, signer1)).toList();
        when(didPublicKeyLoader.loadPublicKey(did)).thenReturn(getVerifier(kp1.getPublic()));
        var pops = Stream.of(pop).toList();
        var errors = proofOfPossessionValidator.validateProofOfPossessionSubmissions(popStringList, pops);
        var objectError = errors
            .getAllErrors()
            .stream()
            .filter(f -> "invalid_nonce_format".equals(f.getCode()))
            .findFirst();
        Assertions.assertThat(objectError).isPresent();
        Assertions.assertThat(objectError.get().getDefaultMessage()).contains("Nonce is not a valid UUID");
    }

    @Test
    void getValidatedProofs_withJwtWithExpiredDate()
        throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, JOSEException {
        var did = "did:example:123";
        var didWithFragment = did + "#key1";
        var pop = new ProofOfPossession(did, UUID.randomUUID().toString());
        var kp1 = generateKeyPair();
        var signer1 = getSigner(kp1.getPrivate());
        var popStringList = Stream.of(
            getPoPSubmission(
                pop.getNonce(),
                did,
                didWithFragment,
                signer1,
                Instant.now().minusSeconds(7200),
                Instant.now().minusSeconds(3600)
            )
        ).toList();
        when(didPublicKeyLoader.loadPublicKey(didWithFragment)).thenReturn(getVerifier(kp1.getPublic()));
        var pops = Stream.of(pop).toList();
        var errors = proofOfPossessionValidator.validateProofOfPossessionSubmissions(popStringList, pops);
        var objectError = errors
            .getAllErrors()
            .stream()
            .filter(f -> "invalid_payload".equals(f.getCode()))
            .findFirst();
        Assertions.assertThat(objectError).isPresent();
        Assertions.assertThat(objectError.get().getDefaultMessage()).contains("Exp is missing or invalid");
    }

    @Test
    void getValidatedProofs_withJwtWithIssuanceInFuture()
        throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, JOSEException {
        var did = "did:example:123";
        var didWithFragment = did + "#key1";
        var pop = new ProofOfPossession(did, UUID.randomUUID().toString());
        var kp1 = generateKeyPair();
        var signer1 = getSigner(kp1.getPrivate());
        var popStringList = Stream.of(
            getPoPSubmission(
                pop.getNonce(),
                did,
                didWithFragment,
                signer1,
                Instant.now().plusSeconds(3600),
                Instant.now().plusSeconds(7200)
            )
        ).toList();
        when(didPublicKeyLoader.loadPublicKey(didWithFragment)).thenReturn(getVerifier(kp1.getPublic()));
        var pops = Stream.of(pop).toList();
        var errors = proofOfPossessionValidator.validateProofOfPossessionSubmissions(popStringList, pops);
        var objectError = errors
            .getAllErrors()
            .stream()
            .filter(f -> "invalid_payload".equals(f.getCode()))
            .findFirst();
        Assertions.assertThat(objectError).isPresent();
        Assertions.assertThat(objectError.get().getDefaultMessage()).contains("Iat is missing or in the future");
    }

    @Test
    void getValidatedProofs_withJwtWithToOld()
        throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, JOSEException {
        var did = "did:example:123";
        var didWithFragment = did + "#key1";
        var pop = new ProofOfPossession(did, UUID.randomUUID().toString());
        var kp1 = generateKeyPair();
        var signer1 = getSigner(kp1.getPrivate());
        var popStringList = Stream.of(
            getPoPSubmission(
                pop.getNonce(),
                did,
                didWithFragment,
                signer1,
                Instant.now().minus(Duration.parse("PT25H")),
                Instant.now().plus(Duration.parse("PT25H"))
            )
        ).toList();
        when(didPublicKeyLoader.loadPublicKey(didWithFragment)).thenReturn(getVerifier(kp1.getPublic()));
        var pops = Stream.of(pop).toList();
        var errors = proofOfPossessionValidator.validateProofOfPossessionSubmissions(popStringList, pops);
        var objectError = errors
            .getAllErrors()
            .stream()
            .filter(f -> "proof_of_possession_too_old".equals(f.getCode()))
            .findFirst();
        Assertions.assertThat(objectError).isPresent();
        Assertions.assertThat(objectError.get().getDefaultMessage()).contains(
            "Issuance and validation can only be apart by"
        );
    }

    @Test
    void getValidatedProofs_withJwtInValidTimeFrame()
        throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, JOSEException {
        var did = "did:example:123";
        var didWithFragment = did + "#key1";
        var pop = new ProofOfPossession(did, UUID.randomUUID().toString());
        var kp1 = generateKeyPair();
        var signer1 = getSigner(kp1.getPrivate());
        var popStringList = Stream.of(
            getPoPSubmission(
                pop.getNonce(),
                did,
                didWithFragment,
                signer1,
                Instant.now().minus(Duration.parse("PT22H")),
                Instant.now().plus(Duration.parse("PT22H"))
            )
        ).toList();
        when(didPublicKeyLoader.loadPublicKey(didWithFragment)).thenReturn(getVerifier(kp1.getPublic()));
        var pops = Stream.of(pop).toList();
        var errors = proofOfPossessionValidator.validateProofOfPossessionSubmissions(popStringList, pops);
        Assertions.assertThat(errors.hasErrors()).isFalse();
    }
}
