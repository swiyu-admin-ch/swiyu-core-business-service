/*
 * SPDX-FileCopyrightText: 2025 Swiss Confederation
 *
 * SPDX-License-Identifier: MIT
 */

package ch.admin.bj.swiyu.core.business.common.did;

import ch.admin.bj.swiyu.core.business.common.exceptions.DidResolveException;
import ch.admin.bj.swiyu.core.business.common.exceptions.MaxSizeApiException;
import ch.admin.bj.swiyu.registry.identifier.IdentifierRegistryProperties;
import ch.admin.eid.did_sidekicks.Jwk;
import ch.admin.eid.didresolver.DidKt;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.text.ParseException;
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
    private final IdentifierRegistryProperties identifierRegistryProperties;

    public DidPublicKeyLoader(
        @Qualifier("didResolverClient") RestClient didResolverClient,
        ObjectMapper objectMapper,
        IdentifierRegistryProperties identifierRegistryProperties
    ) {
        this.didResolverClient = didResolverClient;
        this.objectMapper = objectMapper;
        this.identifierRegistryProperties = identifierRegistryProperties;
    }

    private static JWSVerifier toJwsVerifier(PublicKey publicKey) throws JOSEException {
        if (publicKey instanceof ECPublicKey ecPublicKey) {
            return new ECDSAVerifier(ecPublicKey);
        }
        throw new IllegalArgumentException("Unsupported public key type: " + publicKey.getClass().getName());
    }

    /**
     * Loads the public key of the issuer with the given <code>kid</code>.
     *
     * @return The public key of the issuer.
     * @throws DidResolveException if the public key could not be loaded
     */
    public JWSVerifier loadPublicKey(String kid) throws DidResolveException {
        try {
            log.trace("Fetching Public Key for issuer {}", kid);
            return toJwsVerifier(parsePublicKey(loadJwk(kid)));
        } catch (RuntimeException e) {
            throw new DidResolveException(kid, "Failed to lookup public key from JWT Token", e);
        } catch (JOSEException e) {
            throw new DidResolveException(kid, "Could not parse public key.", e);
        }
    }

    /**
     * Resolves the DID document for the given <code>kid</code> from the base registry and returns the
     * <a href="https://www.w3.org/TR/did-core/#verification-method-properties">verification method</a> key as JWK.
     *
     * @param kid the key id (in jwt token header provided as 'kid' attribute) indicating which verification method to use
     * @return The JWK public key of the verification method identified by the kid.
     */
    private Jwk loadJwk(String kid) throws DidResolveException {
        try (var keyIdAsDid = DidKt.getDidFromAbsoluteKid(kid)) {
            var didUrl = keyIdAsDid.getUrl();
            validateDidUrlIsFromKnownRegistry(didUrl, kid);
            var didLog = didResolverClient.get().uri(didUrl).retrieve().body(String.class);
            if (didLog == null || didLog.isEmpty()) {
                throw new DidResolveException(kid, "DID resolves to empty DidDoc.", null);
            }
            var didDoc = keyIdAsDid.resolveAll(didLog).getDidDoc();
            if (!kid.startsWith(didDoc.getId() + "#")) {
                throw new DidResolveException(
                    kid,
                    "DID resolves to DidDoc which identifies itself as a different DID (%s).".formatted(didDoc.getId()),
                    null
                );
            }
            return didDoc.getKeyByMethodId(kid);
        } catch (MaxSizeApiException e) {
            throw new DidResolveException(kid, "DidDoc exceeds maximum size and cannot be resolved.", e);
        } catch (DidResolveException e) {
            throw e;
        } catch (ch.admin.eid.didresolver.DidResolveException e) {
            throw new DidResolveException(kid, e.getMessage(), e);
        } catch (Exception e) {
            // NOSONAR
            // Should be ch.admin.eid.didresolver.DidResolveException, but not all exceptions are properly wrapped by didresolver lib
            throw new DidResolveException(kid, "Could not resolve key id as DID.", e);
        }
    }

    private void validateDidUrlIsFromKnownRegistry(String didUrl, String kid) {
        var knownRegistryUrlPrefixes = identifierRegistryProperties
            .getPublicResolveUrlTemplates()
            .stream()
            .map(template -> template.contains("%s") ? template.substring(0, template.indexOf("%s")) : template)
            .toList();
        boolean isAllowed = knownRegistryUrlPrefixes.stream().anyMatch(didUrl::startsWith);
        if (!isAllowed) {
            throw new DidResolveException(
                kid,
                "DID URL '%s' does not point to a known registry.".formatted(didUrl),
                null
            );
        }
    }

    /**
     * Generates a public key from the given JSON Web Key (JWK).
     *
     * @param jwk a json web key
     * @return the public key
     */
    private PublicKey parsePublicKey(Jwk jwk) {
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
