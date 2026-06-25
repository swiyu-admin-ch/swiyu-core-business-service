/*
 * SPDX-FileCopyrightText: 2025 Swiss Confederation
 *
 * SPDX-License-Identifier: MIT
 */

package ch.admin.bj.swiyu.core.business.common.did;

import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.admin.bj.swiyu.core.business.test.StatusTestData;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.web.client.RestClient;

class DidPublicKeyLoaderTest {

    private DidPublicKeyLoader publicKeyLoader;
    private RestClient mockedDidResolverAdapter;

    @BeforeEach
    void setUp() {
        mockedDidResolverAdapter = mock(RestClient.class, Answers.RETURNS_DEEP_STUBS);
        publicKeyLoader = new DidPublicKeyLoader(mockedDidResolverAdapter, new ObjectMapper());
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
        Assertions.assertDoesNotThrow(() ->
            publicKeyLoader.loadPublicKey(StatusTestData.VALID_STATUS_LIST_ISSUER_A_KID)
        );
    }
}
