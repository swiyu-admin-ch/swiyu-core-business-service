package ch.admin.bj.swiyu.core.business.modules.status.service;

import static ch.admin.bj.swiyu.core.business.test.StatusTestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import ch.admin.bit.jeap.security.test.WithJeapAuthenticationToken;
import ch.admin.bj.swiyu.core.business.common.config.JsonSchemaConfig;
import ch.admin.bj.swiyu.core.business.common.did.CryptoIntegrityValidator;
import ch.admin.bj.swiyu.core.business.common.did.DidPublicKeyLoader;
import ch.admin.bj.swiyu.core.business.modules.identifier.config.IdentifierSchemaConfig;
import ch.admin.bj.swiyu.core.business.modules.identifier.domain.IdentifierValidator;
import ch.admin.bj.swiyu.core.business.modules.identifier.service.IdentifierEntryMapper;
import ch.admin.bj.swiyu.core.business.modules.identifier.service.IdentifierEntryService;
import ch.admin.bj.swiyu.core.business.modules.status.config.StatusListSchemaConfig;
import ch.admin.bj.swiyu.core.business.modules.status.config.StatusListsLimitProperties;
import ch.admin.bj.swiyu.core.business.modules.status.domain.StatusListEntry;
import ch.admin.bj.swiyu.core.business.modules.status.exceptions.StatusListValidationFailedException;
import ch.admin.bj.swiyu.core.business.test.DataJpaTestConfiguration;
import ch.admin.bj.swiyu.core.business.test.DataJpaTestKafkaConfiguration;
import ch.admin.bj.swiyu.core.business.test.StatusTestData;
import ch.admin.bj.swiyu.core.business.test.TestRepositories;
import ch.admin.bj.swiyu.core.business.test.container.WithAllTestContainerInitializers;
import ch.admin.bj.swiyu.registry.status.StatusRegistryConfig;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.util.unit.DataSize;
import org.springframework.web.client.RestClient;

/**
 * Example of an integration test for a service class without bootstrapping the whole application.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(
    {
        DataJpaTestConfiguration.class,
        DataJpaTestKafkaConfiguration.class,
        JsonSchemaConfig.class,
        StatusRegistryConfig.class,
        StatusListValidator.class,
        StatusListSchemaConfig.class,
        IdentifierSchemaConfig.class,
        IdentifierEntryService.class,
        IdentifierValidator.class,
        IdentifierEntryMapper.class,
        CryptoIntegrityValidator.class,
        DidPublicKeyLoader.class,
    }
)
@WithJeapAuthenticationToken(username = "Test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@WithAllTestContainerInitializers
class StatusListValidatorIT {

    @MockitoSpyBean
    StatusListsLimitProperties statusListsLimitProperties;

    @MockitoBean(answers = Answers.RETURNS_DEEP_STUBS)
    @Qualifier("didResolverClient")
    RestClient didResolverClient;

    // A few tests should skip certain validations
    @MockitoSpyBean
    StatusListValidator statusListValidator;

    @Autowired
    TestRepositories repos;

    @BeforeEach
    void setUp() {
        repos.truncateTables();

        when(
            didResolverClient.get().uri(startsWith(VALID_STATUS_LIST_ISSUER_A_DID_URL)).retrieve().body(String.class)
        ).thenReturn(VALID_STATUS_LIST_ISSUER_A_DID_LOG);

        when(
            didResolverClient.get().uri(startsWith(VALID_STATUS_LIST_ISSUER_B_DID_URL)).retrieve().body(String.class)
        ).thenReturn(VALID_STATUS_LIST_ISSUER_B_DID_LOG);
    }

    @Test
    void statusListInvalid_throwsDidFailedToLoad() {
        // GIVEN
        var entry = new StatusListEntry(VALID_STATUS_LIST_ENTRY_ID_FROM_ISSUER_A, UUID.randomUUID());
        when(
            didResolverClient.get().uri(startsWith(VALID_STATUS_LIST_ISSUER_A_DID_URL)).retrieve().body(String.class)
        ).thenReturn("");

        // WHEN
        var exception = assertThrows(StatusListValidationFailedException.class, () ->
            statusListValidator.validateStatusListVc(entry, VALID_STATUS_LIST_VC_FROM_ISSUER_A)
        );

        // THEN
        assertThat(exception.getMessage()).contains("Provided status list resource is invalid");
        assertThat(exception.getAdditionalDetails()).hasSize(1);
        assertThat(exception.getAdditionalDetails().getFirst()).contains("VC references DID which cannot be resolved.");
    }

    @Test
    void statusListInvalid_throwsWithVcSizeMinLimit() {
        // GIVEN
        var entry = new StatusListEntry(VALID_STATUS_LIST_ENTRY_ID_FROM_ISSUER_A, UUID.randomUUID());
        when(statusListsLimitProperties.minSize()).thenReturn(DataSize.ofBytes(1500));

        // WHEN // THEN
        var exception = assertThrows(StatusListValidationFailedException.class, () ->
            statusListValidator.validateStatusListVc(entry, VALID_STATUS_LIST_VC_FROM_ISSUER_A)
        );

        // THEN Throw general exception
        assertThat(exception.getMessage()).contains("Provided status list resource is invalid");
        assertThat(exception.getAdditionalDetails()).hasSize(1);
        assertThat(exception.getAdditionalDetails().getFirst()).contains("Status list VC size");
    }

    @Test
    void statusListInvalid_throwsWithVcSizeMaxLimit() {
        // GIVEN
        var entry = new StatusListEntry(VALID_STATUS_LIST_ENTRY_ID_FROM_ISSUER_A, UUID.randomUUID());
        when(statusListsLimitProperties.maxSize()).thenReturn(DataSize.ofBytes(10));

        // WHEN // THEN
        var exception = assertThrows(StatusListValidationFailedException.class, () ->
            statusListValidator.validateStatusListVc(entry, VALID_STATUS_LIST_VC_FROM_ISSUER_A)
        );

        // THEN Throw general exception
        assertThat(exception.getMessage()).contains("Provided status list resource is invalid");
        assertThat(exception.getAdditionalDetails()).hasSize(1);
        assertThat(exception.getAdditionalDetails().getFirst()).contains("Status list VC size");
    }

    @Test
    void statusListInvalid_throwsWithBitIncorrect() {
        // GIVEN
        var entry = new StatusListEntry(VALID_STATUS_LIST_ENTRY_ID_FROM_ISSUER_A, UUID.randomUUID());
        // do not check cryptographic integrity as we tampered with the VC to support this scenario
        doNothing().when(statusListValidator).checkStatusListCryptoIntegrity(any());

        // WHEN
        var exception = assertThrows(StatusListValidationFailedException.class, () ->
            statusListValidator.validateStatusListVc(
                entry,
                StatusTestData.INVALID_STATUS_LIST_WITH_WRONG_SIGNAGE_AND_WITH_INVALID_BIT_CONFIGURATION
            )
        );
        // THEN Throw detailed exception
        assertThat(exception.getMessage()).contains("Provided status list resource is invalid");
        assertThat(exception.getAdditionalDetails()).hasSize(1);
        assertThat(exception.getAdditionalDetails().getFirst()).contains(
            "/status_list/bits: does not have a value in the enumeration [1, 2, 4, 8]"
        );
    }

    @Test
    void statusListInvalid_throwsWithJwtInvalid() {
        // GIVEN
        var entry = new StatusListEntry(VALID_STATUS_LIST_ENTRY_ID_FROM_ISSUER_A, UUID.randomUUID());
        // do not check cryptographic integrity as we tampered with the VC to support this scenario
        doNothing().when(statusListValidator).checkStatusListCryptoIntegrity(any());

        // WHEN Wrongly serialized jwt is sent
        var exception = assertThrows(StatusListValidationFailedException.class, () ->
            statusListValidator.validateStatusListVc(
                entry,
                StatusTestData.INVALID_STATUS_LIST_WITH_INVALID_NON_JSON_JWT_PAYLOAD
            )
        );
        // THEN Throw general exception
        assertThat(exception.getMessage()).contains("Provided status list resource is invalid");
        assertThat(exception.getAdditionalDetails()).hasSize(1);
        assertThat(exception.getAdditionalDetails().getFirst()).contains("Unrecognized token");
    }

    @Test
    void statusListInvalid_throwsWithCryptographicIntegrityCompromised() {
        // GIVEN
        var entry = new StatusListEntry(VALID_STATUS_LIST_ENTRY_ID_FROM_ISSUER_A, UUID.randomUUID());

        // WHEN Wrongly signed jwt is sent
        var exception = assertThrows(StatusListValidationFailedException.class, () ->
            statusListValidator.validateStatusListVc(entry, StatusTestData.INVALID_STATUS_LIST_WITH_WRONG_SIGNAGE)
        );

        // THEN Throw general exception
        assertThat(exception.getMessage()).contains("Provided status list resource is invalid");
        assertThat(exception.getAdditionalDetails()).hasSize(1);
        assertThat(exception.getAdditionalDetails().getFirst()).contains("Public key verification failed");
    }

    @Test
    void statusListInvalid_throwsWithStatusListTooOld() {
        // GIVEN
        var entry = new StatusListEntry(VALID_STATUS_LIST_ENTRY_ID_FROM_ISSUER_A, UUID.randomUUID());
        when(statusListsLimitProperties.maxAge()).thenReturn(Duration.ofDays(1));

        // WHEN // THEN
        var exception = assertThrows(StatusListValidationFailedException.class, () ->
            statusListValidator.validateStatusListVc(entry, StatusTestData.VALID_STATUS_LIST_VC_FROM_ISSUER_A)
        );

        // THEN Throw general exception
        assertThat(exception.getMessage()).contains("Provided status list resource is invalid");
        assertThat(exception.getAdditionalDetails()).hasSize(1);
        assertThat(exception.getAdditionalDetails().getFirst()).contains("Statuslist VC is too old.");
    }

    @Test
    void statusListInvalid_throwsWithStatusListNotFromSameBusinessPartner() {
        // GIVEN
        StatusListEntry entry = new StatusListEntry(VALID_STATUS_LIST_ENTRY_ID_FROM_ISSUER_A, UUID.randomUUID());

        // WHEN // THEN
        var exception = assertThrows(StatusListValidationFailedException.class, () ->
            statusListValidator.validateStatusListVc(entry, StatusTestData.VALID_STATUS_LIST_VC_FROM_ISSUER_A)
        );

        // THEN Throw general exception
        assertThat(exception.getMessage()).contains("Provided status list resource is invalid");
        assertThat(exception.getAdditionalDetails()).hasSize(1);
        assertThat(exception.getAdditionalDetails().getFirst()).contains(
            "Statuslist VC is not signed by an issuer belonging to the same business partner."
        );
    }

    @Test
    void statusListInvalid_missingTypHeader() {
        // GIVEN
        var entry = new StatusListEntry(UUID.randomUUID(), UUID.randomUUID());

        // WHEN
        var exception = assertThrows(StatusListValidationFailedException.class, () ->
            statusListValidator.validateStatusListVcV2(entry, INVALID_SWISS_PROFILE_MISSING_TYP_HEADER)
        );

        // THEN
        assertThat(exception.getAdditionalDetails().getFirst()).contains("typ header");
    }

    @Test
    void statusListInvalid_wrongTypHeader() {
        // GIVEN
        var entry = new StatusListEntry(UUID.randomUUID(), UUID.randomUUID());

        // WHEN
        var exception = assertThrows(StatusListValidationFailedException.class, () ->
            statusListValidator.validateStatusListVcV2(entry, INVALID_SWISS_PROFILE_WRONG_TYP_HEADER)
        );

        // THEN
        assertThat(exception.getAdditionalDetails().getFirst()).contains("typ header");
    }

    @Test
    void statusListInvalid_missingProfileVersion() {
        // GIVEN
        var entry = new StatusListEntry(UUID.randomUUID(), UUID.randomUUID());

        // WHEN
        var exception = assertThrows(StatusListValidationFailedException.class, () ->
            statusListValidator.validateStatusListVcV2(entry, INVALID_SWISS_PROFILE_MISSING_PROFILE_VERSION)
        );

        // THEN
        assertThat(exception.getAdditionalDetails().getFirst()).contains("profile_version");
    }

    @Test
    void statusListInvalid_wrongProfileVersion() {
        // GIVEN
        var entry = new StatusListEntry(UUID.randomUUID(), UUID.randomUUID());

        // WHEN
        var exception = assertThrows(StatusListValidationFailedException.class, () ->
            statusListValidator.validateStatusListVcV2(entry, INVALID_SWISS_PROFILE_WRONG_PROFILE_VERSION)
        );

        // THEN
        assertThat(exception.getAdditionalDetails().getFirst()).contains("profile_version");
    }

    @Test
    void statusListInvalid_missingExpClaim() {
        // GIVEN
        var entry = new StatusListEntry(UUID.randomUUID(), UUID.randomUUID());
        doNothing().when(statusListValidator).checkStatusListCryptoIntegrity(any());

        // WHEN
        var exception = assertThrows(StatusListValidationFailedException.class, () ->
            statusListValidator.validateStatusListVcV2(entry, INVALID_SWISS_PROFILE_MISSING_EXP_CLAIM)
        );

        // THEN
        assertThat(exception.getAdditionalDetails().getFirst()).contains("exp claim");
    }

    @Test
    void statusListInvalid_oversizedDecompressedLst() {
        // GIVEN
        var entry = new StatusListEntry(UUID.randomUUID(), UUID.randomUUID());
        doNothing().when(statusListValidator).checkStatusListCryptoIntegrity(any());

        // WHEN
        var exception = assertThrows(StatusListValidationFailedException.class, () ->
            statusListValidator.validateStatusListVcV2(entry, INVALID_SWISS_PROFILE_OVERSIZED_DECOMPRESSED_LST)
        );

        // THEN
        assertThat(exception.getAdditionalDetails().getFirst()).contains("size limit");
    }
}
