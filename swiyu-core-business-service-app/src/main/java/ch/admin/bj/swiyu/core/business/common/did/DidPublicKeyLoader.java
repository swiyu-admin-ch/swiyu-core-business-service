/*
 * SPDX-FileCopyrightText: 2025 Swiss Confederation
 *
 * SPDX-License-Identifier: MIT
 */

package ch.admin.bj.swiyu.core.business.common.did;

import static org.springframework.util.StringUtils.hasText;

import ch.admin.bj.swiyu.core.business.common.exceptions.DidResolveException;
import ch.admin.bj.swiyu.core.business.common.exceptions.MaxSizeApiException;
import ch.admin.eid.did_sidekicks.Jwk;
import ch.admin.eid.did_sidekicks.VerificationMethod;
import ch.admin.eid.did_sidekicks.VerificationType;
import ch.admin.eid.didresolver.Did;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.text.ParseException;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * This class is responsible for loading the public key of an issuer from a JWT Token. The issuer is identified by its
 * DID (Decentralized Identifier) which is stored in the <code>iss</code> claim of the JWT Token. The public key is
 * identified by the <code>kid</code> attribute in the header of the JWT Token.
 * <p>
 * See SD-JWT-based Verifiable Credentials (SD-JWT VC) specification
 * <a href="https://www.ietf.org/archive/id/draft-ietf-oauth-sd-jwt-vc-04.html#section-3.5">Section 3.5 (did document
 * resolution)</a>
 */
@Service
@Slf4j
public class DidPublicKeyLoader {

    private final RestClient didResolverClient;
    private final ObjectMapper objectMapper;

    public DidPublicKeyLoader(@Qualifier("didResolverClient") RestClient didResolverClient, ObjectMapper objectMapper) {
        this.didResolverClient = didResolverClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Decodes a <a href="https://github.com/multiformats/multibase?tab=readme-ov-file#multibase-table">multibase key</a>.
     */
    public static byte[] decodeMultibaseKey(String multikey) {
        var base64UrlEncodedKey = multikey;
        if (!base64UrlEncodedKey.startsWith("u")) {
            throw new UnsupportedOperationException(
                "Failed to decode multikey. Only Base64 Url encoded keys " +
                    "which start with 'u' are supported at the moment."
            );
        }
        base64UrlEncodedKey = base64UrlEncodedKey.substring(1);
        return Base64.getUrlDecoder().decode(base64UrlEncodedKey);
    }

    /**
     * Generates a public key from the given multibase key. The public key is encoded
     * according to the X.509 standard.
     *
     * @param multibaseKey a <a href="https://github.com/multiformats/multibase?tab=readme-ov-file#multibase-table">multibase key</a>
     * @return the public key encoded according to the X.509 standard
     * @throws IllegalArgumentException if the key generation fails due to an invalid key specification or missing algorithm
     */
    private static PublicKey parsePublicKeyOfTypeMultibaseKey(String multibaseKey) {
        if (!hasText(multibaseKey)) {
            throw new IllegalArgumentException(
                "Failed to parse multibase key from verification method since no multibase key was provided"
            );
        }
        try {
            var decodedKey = decodeMultibaseKey(multibaseKey);
            var keyFactory = KeyFactory.getInstance("EC");
            var keySpec = new X509EncodedKeySpec(decodedKey);
            return keyFactory.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(
                "Failed to generate public key from specification due to missing algorithm",
                e
            );
        } catch (InvalidKeySpecException e) {
            throw new IllegalArgumentException(
                "Failed to generate public key from specification due to invalid key spec",
                e
            );
        }
    }

    private static JWSVerifier toJwsVerifier(PublicKey publicKey) throws JOSEException {
        if (publicKey instanceof ECPublicKey ecPublicKey) {
            return new ECDSAVerifier(ecPublicKey);
        }
        throw new IllegalArgumentException("Unsupported public key type: " + publicKey.getClass().getName());
    }

    /**
     * Loads the public key of the issuer with the given <code>issuer</code> and <code>kid</code>.
     *
     * @return The public key of the issuer.
     * @throws DidResolveException if the public key could not be loaded
     */
    public JWSVerifier loadPublicKey(String kid) throws DidResolveException {
        try {
            log.trace("Fetching Public Key for issuer {}", kid);
            VerificationMethod method = loadVerificationMethod(kid);
            return toJwsVerifier(parsePublicKey(method));
        } catch (RuntimeException e) {
            throw new DidResolveException(kid, "Failed to lookup public key from JWT Token", e);
        } catch (JOSEException e) {
            throw new DidResolveException(kid, "Could not parse public key.", e);
        }
    }

    /**
     * Loads the DID document of the given <code>issuerDidTdw</code> from the base registry and returns its
     * <a href="https://www.w3.org/TR/did-core/#verification-method-properties">verification method </a> for the given
     * <code>issuerKeyId</code>. The verification method contains the public key of the issuer.
     *
     * @param didWithVerificationMethodFragment the key id (in jwt token header provided as 'kid' attribute) indicating which verification method to use
     * @return The VerificationMethod The base64 encoded public key of the issuer as it is mentioned in the <code>verificationMethod</code> for the given issuerKeyId.
     */
    private VerificationMethod loadVerificationMethod(String didWithVerificationMethodFragment)
        throws DidResolveException {
        VerificationMethod verificationMethod;
        try (var keyIdAsDid = new Did(didWithVerificationMethodFragment)) {
            var didLog = didResolverClient.get().uri(keyIdAsDid.getUrl()).retrieve().body(String.class);
            if (didLog == null || didLog.isEmpty()) {
                throw new DidResolveException(didWithVerificationMethodFragment, "DID resolves to empty DidDoc.", null);
            }
            var didDoc = keyIdAsDid.resolve(didLog);

            if (!didWithVerificationMethodFragment.startsWith(didDoc.getId() + "#")) {
                throw new DidResolveException(
                    didWithVerificationMethodFragment,
                    "DID resolves to DidDoc which identifies itself as a different DID (%s).".formatted(didDoc.getId()),
                    null
                );
            }

            verificationMethod = didDoc
                .getVerificationMethod()
                .stream()
                .filter(m -> m.getId().equals(didWithVerificationMethodFragment))
                .findFirst()
                .orElseThrow(() ->
                    new DidResolveException(
                        didWithVerificationMethodFragment,
                        "DID references verification method not known to DidDoc.",
                        null
                    )
                );
        } catch (MaxSizeApiException e) {
            throw new DidResolveException(
                didWithVerificationMethodFragment,
                "DidDoc exceeds maximum size and cannot be resolved.",
                e
            );
        } catch (DidResolveException e) {
            throw e;
        } catch (ch.admin.eid.didresolver.DidResolveException e) {
            throw new DidResolveException(didWithVerificationMethodFragment, e.getMessage(), e);
        } catch (Exception e) {
            // NOSONAR
            // Should be ch.admin.eid.didresolver.DidResolveException, but not all exceptions are properly wrapped by didresolver lib
            throw new DidResolveException(didWithVerificationMethodFragment, "Could not resolve key id as DID.", e);
        }
        return verificationMethod;
    }

    /**
     * Generates a public key from the given base64 encoded public key.
     *
     * @param method the verification method containing the public key
     * @return the public key
     * @throws IllegalArgumentException if the key generation fails due to an invalid key specification,
     *                                  missing algorithm or unsupported encoding type
     */
    private PublicKey parsePublicKey(VerificationMethod method) {
        return switch (method.getVerificationType()) {
            case VerificationType.MULTIKEY -> parsePublicKeyOfTypeMultibaseKey(method.getPublicKeyMultibase());
            case VerificationType.JSON_WEB_KEY2020 -> parsePublicKeyOfTypeJsonWebKey(method.getPublicKeyJwk());
            default -> throw new IllegalArgumentException(
                "Unsupported encoding type: " +
                    method.getVerificationType() +
                    ". Only Multikey and JsonWebKey2020 are supported"
            );
        };
    }

    /**
     * Generates a public key from the given JSON Web Key (JWK).
     *
     * @param jwk a json web token
     * @return the public key
     */
    private PublicKey parsePublicKeyOfTypeJsonWebKey(Jwk jwk) {
        if (jwk == null) {
            throw new IllegalArgumentException(
                "Failed to parse Json Web Key from verification method since no jwk was provided"
            );
        }
        try {
            String json = objectMapper.writeValueAsString(jwk);
            return ECKey.parse(json).toECPublicKey();
        } catch (JsonProcessingException | JOSEException | ParseException e) {
            throw new IllegalArgumentException("Failed to parse json web token", e);
        }
    }
}
