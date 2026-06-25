package ch.admin.bj.swiyu.core.business.common.did;

import ch.admin.bj.swiyu.core.business.common.exceptions.CryptoIntegrityValidationFailedException;
import ch.admin.bj.swiyu.core.business.common.exceptions.DidResolveException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CryptoIntegrityValidator {

    private final DidPublicKeyLoader didPublicKeyLoader;

    public void checkJwtCryptoIntegrity(SignedJWT vc) throws CryptoIntegrityValidationFailedException {
        var keyId = vc.getHeader().getKeyID();
        if (keyId == null || keyId.isEmpty()) {
            throw new CryptoIntegrityValidationFailedException("VC does not contain a key id.", null);
        }

        try {
            var publicKey = didPublicKeyLoader.loadPublicKey(keyId);

            if (!vc.verify(publicKey)) {
                throw new CryptoIntegrityValidationFailedException("Public key verification failed", null);
            }
        } catch (DidResolveException e) {
            throw new CryptoIntegrityValidationFailedException("VC references DID which cannot be resolved.", e);
        } catch (JOSEException e) {
            throw new CryptoIntegrityValidationFailedException("VC could not be parsed.", e);
        }
    }
}
