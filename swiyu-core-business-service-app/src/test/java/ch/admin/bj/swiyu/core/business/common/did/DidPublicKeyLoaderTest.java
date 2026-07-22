/*
 * SPDX-FileCopyrightText: 2025 Swiss Confederation
 *
 * SPDX-License-Identifier: MIT
 */

package ch.admin.bj.swiyu.core.business.common.did;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.admin.bj.swiyu.core.business.common.exceptions.DidResolveException;
import ch.admin.bj.swiyu.core.business.test.StatusTestData;
import ch.admin.bj.swiyu.registry.identifier.IdentifierRegistryProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.web.client.RestClient;

class DidPublicKeyLoaderTest {

    private static final String ALLOWED_REGISTRY_TEMPLATE =
        "https://identifier-reg-r.trust-infra.swiyu.admin.ch/api/v1/did/%s";

    private DidPublicKeyLoader publicKeyLoader;
    private RestClient mockedDidResolverAdapter;

    @BeforeEach
    void setUp() {
        mockedDidResolverAdapter = mock(RestClient.class, Answers.RETURNS_DEEP_STUBS);
        var registryProperties = mock(IdentifierRegistryProperties.class);
        when(registryProperties.getPublicResolveUrlTemplates()).thenReturn(List.of(ALLOWED_REGISTRY_TEMPLATE));
        publicKeyLoader = new DidPublicKeyLoader(mockedDidResolverAdapter, new ObjectMapper(), registryProperties);
    }

    @Test
    void loadPublicKey_JsonWebKey() {
        // GIVEN (an issuer registered in the DID registry and an issuer signed SD-JWT)
        when(
            mockedDidResolverAdapter
                .get()
                .uri(startsWith(StatusTestData.VALID_STATUS_LIST_ISSUER_A_DID_URL))
                .retrieve()
                .body(String.class)
        ).thenReturn(StatusTestData.VALID_STATUS_LIST_ISSUER_A_DID_LOG);

        // WHEN / THEN
        assertDoesNotThrow(() -> publicKeyLoader.loadPublicKey(StatusTestData.VALID_STATUS_LIST_ISSUER_A_KID));
    }

    @Test
    void loadPublicKey_rejectsDidUrlNotInAllowedRegistry() {
        // GIVEN a kid whose DID resolves to a URL not in the allowed registry list
        var unknownRegistryKid =
            "did:tdw:QmbBoyVLWetfXMKwsrtZcejKVKhMY5nVy138R7F9bQwxtw:unknown-registry.example.com:api:v1:did:1abc96db-2ade-4b6c-baaf-b4f461cdabed#assert-key-01";

        // WHEN / THEN
        assertThrows(DidResolveException.class, () -> publicKeyLoader.loadPublicKey(unknownRegistryKid));
    }
}
