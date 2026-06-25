/*
 * SPDX-FileCopyrightText: 2025 Swiss Confederation
 *
 * SPDX-License-Identifier: MIT
 */

package ch.admin.bj.swiyu.core.business.common.did;

import static ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding.ProofOfPossessionKeyUtils.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.admin.bj.swiyu.core.business.common.exceptions.CryptoIntegrityValidationFailedException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.SignedJWT;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CryptoIntegrityValidatorTest {

    DidPublicKeyLoader didPublicKeyLoader;

    CryptoIntegrityValidator cryptoIntegrityValidator;

    @BeforeEach
    void beforeEach() {
        didPublicKeyLoader = mock(DidPublicKeyLoader.class);
        cryptoIntegrityValidator = new CryptoIntegrityValidator(didPublicKeyLoader);
    }

    @Test
    void validate_failsWithEmptyKeyId()
        throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, JOSEException, ParseException {
        var did = "did:example:123";
        var kp1 = generateKeyPair();
        var signer1 = getSigner(kp1.getPrivate());
        var jwt = getPoPSubmission("nonce", did, "", signer1);
        var signedJwt = SignedJWT.parse(jwt);

        Assertions.assertThatExceptionOfType(CryptoIntegrityValidationFailedException.class)
            .isThrownBy(() -> cryptoIntegrityValidator.checkJwtCryptoIntegrity(signedJwt))
            .withMessageContaining("VC does not contain a key id.");
    }

    @Test
    void validate_failsWithMismatchingKeyMaterialId()
        throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, JOSEException, ParseException {
        var kp1 = generateKeyPair();
        var kp2 = generateKeyPair();
        var signer1 = getSigner(kp1.getPrivate());
        var verifier2 = getVerifier(kp2.getPublic());
        var jwt = getPoPSubmission("nonce", "did:example:123", "did:example:123#suffix", signer1);
        var signedJwt = SignedJWT.parse(jwt);
        when(didPublicKeyLoader.loadPublicKey("did:example:123#suffix")).thenReturn(verifier2);

        Assertions.assertThatExceptionOfType(CryptoIntegrityValidationFailedException.class)
            .isThrownBy(() -> cryptoIntegrityValidator.checkJwtCryptoIntegrity(signedJwt))
            .withMessageContaining("Public key verification failed");
    }
}
