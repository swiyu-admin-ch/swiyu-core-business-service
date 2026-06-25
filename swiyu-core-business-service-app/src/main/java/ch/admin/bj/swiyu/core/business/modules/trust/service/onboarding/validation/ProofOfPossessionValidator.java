package ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding.validation;

import ch.admin.bj.swiyu.core.business.common.did.CryptoIntegrityValidator;
import ch.admin.bj.swiyu.core.business.common.did.DidUtil;
import ch.admin.bj.swiyu.core.business.common.exceptions.CryptoIntegrityValidationFailedException;
import ch.admin.bj.swiyu.core.business.modules.trust.api.ProofOfPossessionValidatorErrorCodeDto;
import ch.admin.bj.swiyu.core.business.modules.trust.config.TrustOnboardingSubmissionProofOfPossessionProperties;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.ProofOfPossession;
import com.nimbusds.jwt.SignedJWT;
import java.text.ParseException;
import java.time.Instant;
import java.util.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.SimpleErrors;

@Slf4j
@Component
@AllArgsConstructor
public class ProofOfPossessionValidator {

    private final CryptoIntegrityValidator cryptoIntegrityValidator;
    private final TrustOnboardingSubmissionProofOfPossessionProperties popProperties;

    public boolean isDidSelectionEqual(List<ProofOfPossession> currentPops, List<String> targetDids) {
        var existingDids = currentPops.stream().map(ProofOfPossession::getDid).sorted().toList();
        var newDids = targetDids != null ? targetDids.stream().sorted().toList() : List.<String>of();
        return existingDids.equals(newDids);
    }

    public Errors validateProofOfPossessionSubmissions(
        List<String> jwtSubmission,
        List<ProofOfPossession> requiredProofOfPossessions
    ) {
        Errors errors = new SimpleErrors(requiredProofOfPossessions);
        Map<String, SignedJWT> jwts = new HashMap<>();
        // First parse and validate all provided JWTs
        for (String jwt : jwtSubmission) {
            try {
                var signedJwt = SignedJWT.parse(jwt);
                var crpyotErrors = validateProofOfPossessionCryptoIntegrity(signedJwt);
                var payloadErrors = validateTokenPayload(signedJwt);
                if (crpyotErrors.hasErrors() || payloadErrors.hasErrors()) {
                    errors.addAllErrors(crpyotErrors);
                    errors.addAllErrors(payloadErrors);
                    continue;
                }
                var did = DidUtil.getDidFromKeyId(signedJwt.getHeader().getKeyID());
                if (did != null) {
                    jwts.put(did, signedJwt);
                }
            } catch (ParseException _) {
                errors.reject(
                    ProofOfPossessionValidatorErrorCodeDto.INVALID_JWT.toString(),
                    "Couldn't parse provided proof of possession jwt"
                );
            }
        }

        // Then check if all required PoPs are provided and match the nonce
        for (ProofOfPossession pop : requiredProofOfPossessions) {
            var jwt = jwts.remove(pop.getDid());
            if (jwt == null) {
                errors.reject(
                    ProofOfPossessionValidatorErrorCodeDto.INVALID_DID.toString(),
                    "No proof of possession provided for DID: " + pop.getDid()
                );
                continue;
            }
            try {
                String nonce = jwt.getJWTClaimsSet().getClaimAsString("nonce");
                if (!pop.getNonce().equals(nonce)) {
                    errors.reject(
                        ProofOfPossessionValidatorErrorCodeDto.MISMATCHING_NONCE.toString(),
                        "Mismatching nonce in proof of possession for DID: " + pop.getDid()
                    );
                }
            } catch (ParseException _) {
                errors.reject(
                    ProofOfPossessionValidatorErrorCodeDto.INVALID_JWT.toString(),
                    "Couldn't extract proof of possession jwt claimset for DID: " + pop.getDid()
                );
            }
        }

        return errors;
    }

    private Errors validateTokenPayload(SignedJWT signedJwt) {
        Errors errors = new SimpleErrors(signedJwt);
        try {
            // Check nonce
            var nonce = signedJwt.getJWTClaimsSet().getClaimAsString("nonce");
            if (nonce == null || nonce.isEmpty()) {
                errors.reject(
                    ProofOfPossessionValidatorErrorCodeDto.MISSING_NONCE.toString(),
                    "Nonce is missing in the proof of possession"
                );
                return errors;
            }
            UUID.fromString(nonce);

            var now = new Date();
            var iat = signedJwt.getJWTClaimsSet().getIssueTime();
            var exp = signedJwt.getJWTClaimsSet().getExpirationTime();
            if (iat == null || iat.after(now)) {
                errors.reject(
                    ProofOfPossessionValidatorErrorCodeDto.INVALID_PAYLOAD.toString(),
                    "Iat is missing or in the future"
                );
                return errors;
            }
            if (exp == null || exp.before(now)) {
                errors.reject(
                    ProofOfPossessionValidatorErrorCodeDto.INVALID_PAYLOAD.toString(),
                    "Exp is missing or invalid"
                );
                return errors;
            }
            if (Instant.now().isAfter(iat.toInstant().plus(popProperties.issuanceToValidityDuration()))) {
                errors.reject(
                    ProofOfPossessionValidatorErrorCodeDto.PROOF_OF_POSSESSION_TOO_OLD.toString(),
                    String.format(
                        "Issuance and validation can only be apart by %d seconds",
                        popProperties.issuanceToValidityDuration().toSeconds()
                    )
                );
            }
        } catch (ParseException _) {
            errors.reject(
                ProofOfPossessionValidatorErrorCodeDto.INVALID_JWT.toString(),
                "Couldn't extract proof of possession jwt claimset"
            );
        } catch (IllegalArgumentException _) {
            errors.reject(
                ProofOfPossessionValidatorErrorCodeDto.INVALID_NONCE_FORMAT.toString(),
                "Nonce is not a valid UUID"
            );
        }
        return errors;
    }

    private Errors validateProofOfPossessionCryptoIntegrity(SignedJWT popJwt) {
        Errors errors = new SimpleErrors(popJwt);
        try {
            cryptoIntegrityValidator.checkJwtCryptoIntegrity(popJwt);
        } catch (CryptoIntegrityValidationFailedException e) {
            errors.reject(
                ProofOfPossessionValidatorErrorCodeDto.INVALID_CRYPTO_INTEGRITY.toString(),
                String.join("", e.getAdditionalDetails())
            );
        }
        return errors;
    }
}
