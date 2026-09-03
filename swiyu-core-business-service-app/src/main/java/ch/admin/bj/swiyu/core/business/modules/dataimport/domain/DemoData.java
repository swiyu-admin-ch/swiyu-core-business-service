package ch.admin.bj.swiyu.core.business.modules.dataimport.domain;

import static ch.admin.bj.swiyu.core.business.common.service.LocalizedMapUtil.fromLanguages;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;

/*
 * This class must be kept synchronized between CBS and TMS.
 * If something change on one side, it must be pushed on the other.
 * CBS: ch.admin.bj.swiyu.core.business.modules.dataimport.domain.DemoData
 * TMS: ch.admin.bj.swiyu.trust.management.modules.dataimport.domain.DemoData
 */
@SuppressWarnings(
    {
        "java:S1192", // Allow text repeats in demo data
        "java:S2386", // Sonar does not catch that the maps are unmodifiable
    }
)
@UtilityClass
public class DemoData {

    @RequiredArgsConstructor
    public enum DemoCase {
        /**
         * Status:<br/>
         * identifier registry: <font color="green">onboarded</font><br/>
         * trust registry: <font color="green">onboarded</font><br/>
         * <!-- Should be one of: -->
         * <!-- <font color="green">onboarded</font> -->
         * <!-- <font color="orange">ongoing</font> -->
         * <!-- <font color="red">NOT onboarded</font> -->
         * <p>
         * Scenario:<br/>
         * Governmental BP which is already onboarded to Trust Registry.<br/>
         * Has authorization to verify a ProtectedVerification.<br/>
         * Has authorization to issue a ProtectedIssuance.<br/>
         */
        GOV_BP_TRUST_ONBOARDING(
            DemoBusinessPartner.of(
                DemoBusinessPartner.DemoBusinessPartnerType.GOVERNMENTAL_INSTITUTION,
                "77054cd8-6fe6-44eb-be22-b56ebf9c8622",
                "erika.mueller@e2e.local",
                fromLanguages(
                    "Trusted Gov Partner",
                    "Trusted Gov Partner (DE)",
                    "Trusted Gov Partner (FR)",
                    "Trusted Gov Partner (IT)",
                    "Trusted Gov Partner (EN)",
                    "Trusted Gov Partner (RM)"
                ),
                new DemoBusinessPartner.DemoAddress("Geschäftsstraße 13", "Demohausen", "1111", "CH", "Democanton"),
                new DemoBusinessPartner.DemoContact(
                    "erika",
                    "müller",
                    "erika.mueller@trusty-consulting.local",
                    "+41548884440",
                    DemoBusinessPartner.DemoContact.Language.DE
                ),
                DemoBusinessPartner.DemoSigningRule.SINGLE_SIGNATURE,
                List.of(
                    new DemoBusinessPartner.DemoSignatory(
                        "Erika",
                        "Müller",
                        "+41776665544",
                        "erika.mueller@trusty-consulting.local"
                    )
                ),
                null,
                "+41791234567",
                null,
                List.of(
                    new DemoBusinessPartner.DemoIdentifier(
                        UUID.fromString("effaab62-ab2d-4794-ba7f-48cbbe3ea383"),
                        "did:webvh:QmSa8SMKvTxwJxavsHP6uxsrVL4B7eTPMVHct8LbT9yTty:localhost%3A8190:api:v1:did:effaab62-ab2d-4794-ba7f-48cbbe3ea383",
                        "Did for gov trusted",
                        "{\"versionId\":\"1-QmSfpPzd7kBQ6DPuccaJjBW9m6nScfNYnPuEduoxSFkD26\",\"versionTime\":\"2026-08-06T11:08:35Z\",\"parameters\":{\"method\":\"did:webvh:1.0\",\"scid\":\"QmSa8SMKvTxwJxavsHP6uxsrVL4B7eTPMVHct8LbT9yTty\",\"updateKeys\":[\"z6Mkozy1Dpit4opQXbfLthUM5KdZDoQLzMGzN3jLes6KahrQ\"],\"portable\":false},\"state\":{\"id\":\"did:webvh:QmSa8SMKvTxwJxavsHP6uxsrVL4B7eTPMVHct8LbT9yTty:localhost%3A8190:api:v1:did:effaab62-ab2d-4794-ba7f-48cbbe3ea383\",\"profile_version\":\"swiss-profile-anchor:1.0.0\",\"authentication\":[\"did:webvh:QmSa8SMKvTxwJxavsHP6uxsrVL4B7eTPMVHct8LbT9yTty:localhost%3A8190:api:v1:did:effaab62-ab2d-4794-ba7f-48cbbe3ea383#auth-key-01\"],\"assertionMethod\":[\"did:webvh:QmSa8SMKvTxwJxavsHP6uxsrVL4B7eTPMVHct8LbT9yTty:localhost%3A8190:api:v1:did:effaab62-ab2d-4794-ba7f-48cbbe3ea383#assert-key-01\"],\"verificationMethod\":[{\"id\":\"did:webvh:QmSa8SMKvTxwJxavsHP6uxsrVL4B7eTPMVHct8LbT9yTty:localhost%3A8190:api:v1:did:effaab62-ab2d-4794-ba7f-48cbbe3ea383#auth-key-01\",\"controller\":\"did:webvh:QmSa8SMKvTxwJxavsHP6uxsrVL4B7eTPMVHct8LbT9yTty:localhost%3A8190:api:v1:did:effaab62-ab2d-4794-ba7f-48cbbe3ea383\",\"type\":\"JsonWebKey2020\",\"publicKeyJwk\":{\"kty\":\"EC\",\"crv\":\"P-256\",\"x\":\"1rj9SCVfrol4JSYUXPFH50MSl28QuP1lRslb9C3jvi8\",\"y\":\"Pmit2QvqFNEAT9kKhVpfN7vSLk0sbzTVsun4OKO8o9g\",\"kid\":\"auth-key-01\"}},{\"id\":\"did:webvh:QmSa8SMKvTxwJxavsHP6uxsrVL4B7eTPMVHct8LbT9yTty:localhost%3A8190:api:v1:did:effaab62-ab2d-4794-ba7f-48cbbe3ea383#assert-key-01\",\"controller\":\"did:webvh:QmSa8SMKvTxwJxavsHP6uxsrVL4B7eTPMVHct8LbT9yTty:localhost%3A8190:api:v1:did:effaab62-ab2d-4794-ba7f-48cbbe3ea383\",\"type\":\"JsonWebKey2020\",\"publicKeyJwk\":{\"kty\":\"EC\",\"crv\":\"P-256\",\"x\":\"hfLUyfOj8tNfzKybUKCUfUh7WWvxdQCbZiYn94kmYFY\",\"y\":\"ITknv43a5zNcSIR5pL3XZLzru905EuUAdMZyC4a5Bz4\",\"kid\":\"assert-key-01\"}}]},\"proof\":[{\"type\":\"DataIntegrityProof\",\"cryptosuite\":\"eddsa-jcs-2022\",\"created\":\"2026-08-06T11:08:35Z\",\"verificationMethod\":\"did:key:z6Mkozy1Dpit4opQXbfLthUM5KdZDoQLzMGzN3jLes6KahrQ#z6Mkozy1Dpit4opQXbfLthUM5KdZDoQLzMGzN3jLes6KahrQ\",\"proofPurpose\":\"assertionMethod\",\"proofValue\":\"z2AoWEb9hZ7CQVdvvFMxEQn2NkZRd8K6xx4UPx9nJahYvmAy1LaDcfNQJ9DmAAKvTbEkKwun2BgfSMoFm81MxDDzS\"}]}",
                        true
                    )
                ),
                List.of(
                    new DemoBusinessPartner.DemoTrustOnboarding(
                        UUID.fromString("77cd5ae5-c553-4440-b1b7-57cb8f4af6f4"),
                        DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingSubmissionStatus.SUCCEEDED,
                        List.of(
                            new DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingSubmissionDocument(
                                "Declaration of intent.pdf",
                                DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingSubmissionDocument.DemoTrustOnboardingSubmissionDocumentTypeDto.TRUST_ONBOARDING_DECLARATION_OF_INTENT,
                                "something"
                            )
                        ),
                        new DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingTask(
                            UUID.fromString("b8945a81-b6ca-41cd-8691-9bbfd02d4fcb"),
                            DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingTask.DemoTrustTaskStatus.ACCEPTED,
                            Instant.now().minus(5, ChronoUnit.DAYS),
                            Instant.now().minus(35, ChronoUnit.DAYS)
                        )
                    )
                ),
                DemoBusinessPartner.DemoBusinessPartnerIdentity.of(
                    DemoBusinessPartner.DemoBusinessPartnerIdentity.DemoBusinessPartnerIdentityStatus.ACTIVE,
                    List.of(
                        new DemoBusinessPartner.DemoBusinessPartnerIdentity.DemoProtectedVerificationAuthorization(
                            UUID.fromString("e95dbfcc-0a16-4788-b985-a3aeb7ce9342"),
                            DemoBusinessPartner.DemoBusinessPartnerIdentity.DemoProtectedVerificationAuthorization.DemoProtectedVerificationField.AHV_NUMBER
                        )
                    ),
                    List.of(
                        new DemoBusinessPartner.DemoBusinessPartnerIdentity.DemoProtectedIssuanceAuthorization(
                            UUID.fromString("6ea9eeb8-f5fb-4aad-b3dc-f836ff85aca7"),
                            "did:webvh:QmSa8SMKvTxwJxavsHP6uxsrVL4B7eTPMVHct8LbT9yTty:localhost%3A8190:api:v1:did:effaab62-ab2d-4794-ba7f-48cbbe3ea383",
                            fromLanguages(
                                "Test reason (DEFAULT)",
                                "Test reason (DE)",
                                "Test reason (FR)",
                                "Test reason (IT)",
                                "Test reason (EN)",
                                "Test reason (RM)"
                            ),
                            UUID.fromString("1ef66a61-ab48-4a55-a4a1-b1423da05d6f"),
                            "urn:vct:demodata:protected:issuance:1ef66a61-ab48-4a55-a4a1-b1423da05d6f",
                            fromLanguages(
                                "DemoData (DEFAULT)",
                                "DemoData (DE)",
                                "DemoData (FR)",
                                "DemoData (IT)",
                                "DemoData (EN)",
                                "DemoData (RM)"
                            )
                        )
                    )
                ),
                List.of(
                    // The request that led to the AHV_NUMBER authorization above - an authorization can only ever
                    // be created by approving one of these, so this one must stay in sync with it.
                    new DemoBusinessPartner.DemoProtectedVerificationSubmission(
                        UUID.fromString("f4a1e295-6e83-4a13-8e3c-4b6f9a0e6d31"),
                        UUID.fromString("319bdaeb-1611-46e9-bff7-dd2dbba45d74"), // real SBN-ID, Helsana Versicherungen AG
                        DemoBusinessPartner.DemoProtectedVerificationSubmission.DemoProtectedVerificationCategory.PERSONAL_ADMINISTRATIVE_NUMBER,
                        DemoBusinessPartner.DemoProtectedVerificationSubmission.DemoProtectedVerificationSubmissionStatus.APPROVED,
                        "Verify AHV number for onboarding",
                        new DemoBusinessPartner.DemoProtectedVerificationSubmission.DemoProtectedVerificationRequestTask(
                            DemoBusinessPartner.DemoProtectedVerificationSubmission.DemoProtectedVerificationRequestTask.DemoProtectedVerificationTaskStatus.ACCEPTED,
                            Instant.now().minus(5, ChronoUnit.DAYS),
                            Instant.now().minus(35, ChronoUnit.DAYS)
                        )
                    )
                )
            )
        ),
        /**
         * Status:<br/>
         * identifier registry: <font color="green">onboarded</font><br/>
         * trust registry: <font color="green">onboarded</font><br/>
         * <!-- Should be one of: -->
         * <!-- <font color="green">onboarded</font> -->
         * <!-- <font color="orange">ongoing</font> -->
         * <!-- <font color="red">NOT onboarded</font> -->
         * <p>
         * Scenario:<br/>
         * Business BP which is already onboarded to Trust Registry, but has no protected verification history
         * at all yet - no authorization, no submitted or approved request. Use this one (instead of
         * {@link #GOV_BP_TRUST_ONBOARDING}) to test submitting and approving a protected verification request
         * from a clean state, since GOV_BP_TRUST_ONBOARDING already has both seeded.<br/>
         */
        BP_VERIFIED_NO_PROTECTED_VERIFICATION(
            DemoBusinessPartner.of(
                DemoBusinessPartner.DemoBusinessPartnerType.BUSINESS,
                "e2f0a7d4-7b91-4c3e-9c04-1a6b8f2d5e73",
                "fresh.partner@e2e.test",
                fromLanguages(
                    "Fresh Verified Partner AG",
                    "Fresh Verified Partner AG (DE)",
                    "Fresh Verified Partner SA (FR)",
                    "Fresh Verified Partner S.r.l. (IT)",
                    "Fresh Verified Partner AG (EN)",
                    "Fresh Verified Partner AG (RM)"
                ),
                new DemoBusinessPartner.DemoAddress("Geschäftsstraße 13", "Demohausen", "1111", "CH", "Democanton"),
                new DemoBusinessPartner.DemoContact(
                    "erika",
                    "müller",
                    "erika.mueller@trusty-consulting.com",
                    "+41548884440",
                    DemoBusinessPartner.DemoContact.Language.DE
                ),
                DemoBusinessPartner.DemoSigningRule.SINGLE_SIGNATURE,
                List.of(
                    new DemoBusinessPartner.DemoSignatory(
                        "Erika",
                        "Müller",
                        "+41776665544",
                        "erika.mueller@trusty-consulting.com"
                    )
                ),
                null,
                "+41791234567",
                null,
                null,
                List.of(
                    new DemoBusinessPartner.DemoTrustOnboarding(
                        UUID.fromString("b6d3c9a1-4e27-4f8a-8e15-2c7d9a3f6b48"),
                        DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingSubmissionStatus.SUCCEEDED,
                        List.of(
                            new DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingSubmissionDocument(
                                "Declaration of intent.pdf",
                                DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingSubmissionDocument.DemoTrustOnboardingSubmissionDocumentTypeDto.TRUST_ONBOARDING_DECLARATION_OF_INTENT,
                                "something"
                            )
                        ),
                        new DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingTask(
                            UUID.fromString("d1a5e8c3-9f42-4b76-a1d8-6e0f3c8b2a95"),
                            DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingTask.DemoTrustTaskStatus.ACCEPTED,
                            Instant.now().minus(5, ChronoUnit.DAYS),
                            Instant.now().minus(35, ChronoUnit.DAYS)
                        )
                    )
                ),
                DemoBusinessPartner.DemoBusinessPartnerIdentity.of(
                    DemoBusinessPartner.DemoBusinessPartnerIdentity.DemoBusinessPartnerIdentityStatus.ACTIVE,
                    List.of(),
                    List.of()
                )
            )
        ),
        /**
         * Status:<br/>
         * identifier registry: <font color="green">onboarded</font><br/>
         * trust registry: <font color="orange">NOT onboarded</font><br/>
         * <!-- Should be one of: -->
         * <!-- <font color="green">onboarded</font> -->
         * <!-- <font color="orange">ongoing</font> -->
         * <!-- <font color="red">NOT onboarded</font> -->
         * <p>
         * Scenario:<br/>
         * A BP which is used in dynamic E2E tests.<br/>
         */
        E2ETEST_BP(
            DemoBusinessPartner.of(
                DemoBusinessPartner.DemoBusinessPartnerType.GOVERNMENTAL_INSTITUTION,
                "7b24f978-afe7-4f60-af2f-57ae5a01d303",
                "erika.mueller@e2e.local",
                fromLanguages(
                    "E2e GmbH (DE)",
                    "E2e GmbH (DE)",
                    "E2e GmbH (FR)",
                    "E2e S.r.l. (IT)",
                    "E2e GmbH (EN)",
                    "E2e GmbH (RM)"
                ),
                new DemoBusinessPartner.DemoAddress("Geschäftsstraße 13", "Demohausen", "1111", "CH", "Democanton"),
                new DemoBusinessPartner.DemoContact(
                    "Erika",
                    "Muster",
                    "erika.mueller@e2e.local",
                    "+41548884440",
                    DemoBusinessPartner.DemoContact.Language.DE
                ),
                null,
                null,
                null,
                "+41548884440",
                null,
                List.of(
                    new DemoBusinessPartner.DemoIdentifier(
                        UUID.fromString("1133c5ee-f7b2-400c-a358-3bae4bba7672"),
                        "did:webvh:QmPidcC5K8DxBHUCTwn12YCYK6VAkAc3EyEZpsbMaHooyy:localhost%3A8190:api:v1:did:1133c5ee-f7b2-400c-a358-3bae4bba7672",
                        "E2E Test DID for localhost",
                        "{\"versionId\":\"1-QmStVvgLKv3oVnerc74p54pt8UcF8urHH32t9LvDVYLhMu\",\"versionTime\":\"2026-07-07T15:10:31Z\",\"parameters\":{\"method\":\"did:webvh:1.0\",\"scid\":\"QmPidcC5K8DxBHUCTwn12YCYK6VAkAc3EyEZpsbMaHooyy\",\"updateKeys\":[\"z6MknJEsZ8hynVFFF1oFFrJEdtsgTtSjuaE3k3RtpJfzLK9G\"],\"portable\":false},\"state\":{\"id\":\"did:webvh:QmPidcC5K8DxBHUCTwn12YCYK6VAkAc3EyEZpsbMaHooyy:localhost%3A8190:api:v1:did:1133c5ee-f7b2-400c-a358-3bae4bba7672\",\"profile_version\":\"swiss-profile-anchor:1.0.0\",\"authentication\":[\"did:webvh:QmPidcC5K8DxBHUCTwn12YCYK6VAkAc3EyEZpsbMaHooyy:localhost%3A8190:api:v1:did:1133c5ee-f7b2-400c-a358-3bae4bba7672#auth-key-01\"],\"assertionMethod\":[\"did:webvh:QmPidcC5K8DxBHUCTwn12YCYK6VAkAc3EyEZpsbMaHooyy:localhost%3A8190:api:v1:did:1133c5ee-f7b2-400c-a358-3bae4bba7672#assert-key-01\"],\"verificationMethod\":[{\"id\":\"did:webvh:QmPidcC5K8DxBHUCTwn12YCYK6VAkAc3EyEZpsbMaHooyy:localhost%3A8190:api:v1:did:1133c5ee-f7b2-400c-a358-3bae4bba7672#auth-key-01\",\"controller\":\"did:webvh:QmPidcC5K8DxBHUCTwn12YCYK6VAkAc3EyEZpsbMaHooyy:localhost%3A8190:api:v1:did:1133c5ee-f7b2-400c-a358-3bae4bba7672\",\"type\":\"JsonWebKey2020\",\"publicKeyJwk\":{\"kty\":\"EC\",\"crv\":\"P-256\",\"x\":\"lJHTz-hxyWyudXR-Ik3n59Njh0ZDe67LOAC4rWtGwVI\",\"y\":\"UQ-GiYrC3ZPwVhKNc00u1-QHYXRU1tIz8_KtkC-MGSs\",\"kid\":\"auth-key-01\"}},{\"id\":\"did:webvh:QmPidcC5K8DxBHUCTwn12YCYK6VAkAc3EyEZpsbMaHooyy:localhost%3A8190:api:v1:did:1133c5ee-f7b2-400c-a358-3bae4bba7672#assert-key-01\",\"controller\":\"did:webvh:QmPidcC5K8DxBHUCTwn12YCYK6VAkAc3EyEZpsbMaHooyy:localhost%3A8190:api:v1:did:1133c5ee-f7b2-400c-a358-3bae4bba7672\",\"type\":\"JsonWebKey2020\",\"publicKeyJwk\":{\"kty\":\"EC\",\"crv\":\"P-256\",\"x\":\"b8iC-SjRKmuoSIGDhD7C7GTiSq06aIyx07yI9WlryF4\",\"y\":\"GsyVRZcIyc6pcXKClxTiwXGTCcXZf6sWXA8tQmWgRBI\",\"kid\":\"assert-key-01\"}}]},\"proof\":[{\"type\":\"DataIntegrityProof\",\"cryptosuite\":\"eddsa-jcs-2022\",\"created\":\"2026-07-07T15:10:31Z\",\"verificationMethod\":\"did:key:z6MknJEsZ8hynVFFF1oFFrJEdtsgTtSjuaE3k3RtpJfzLK9G#z6MknJEsZ8hynVFFF1oFFrJEdtsgTtSjuaE3k3RtpJfzLK9G\",\"proofPurpose\":\"assertionMethod\",\"proofValue\":\"z4FaBXsyaD4ZXY3WYTsjiPLp4N2HDGfz6ax2sELpxLMrJrTFyk5T6MhgEUCrfyPPxPsicNdqPRSiXFMLFWREpoYkp\"}]}",
                        false
                    )
                ),
                List.of(),
                null
            )
        ),
        /**
         * Status:<br/>
         * identifier registry: <font color="green">onboarded</font><br/>
         * trust registry: <font color="orange">ongoing</font><br/>
         * <!-- Should be one of: -->
         * <!-- <font color="green">onboarded</font> -->
         * <!-- <font color="orange">ongoing</font> -->
         * <!-- <font color="red">NOT onboarded</font> -->
         * <p>
         * Scenario:<br/>
         * A BP which already is onboarding to the trust registry but already send out a new Trust .<br/>
         * TMS responded with a request for more information.<br/>
         */
        BUSINESS_BP_TRUST_ONBOARDING_RE_VERIFICATION(
            DemoBusinessPartner.of(
                DemoBusinessPartner.DemoBusinessPartnerType.BUSINESS,
                // Keep ID in sync with
                // ch.admin.bj.swiyu.core.business.common.demodata.DemoDataConstants.BusinessPartner#CORE_ID_BP_DEFAULT
                "9f425029-9775-4984-99ba-bacc60069502",
                "erika.mueller@trusty-consulting.local",
                fromLanguages(
                    "Vertrau mir Beratung GmbH",
                    "Vertrau mir Beratung GmbH (DE)",
                    "Confiance Conseil GmbH (FR)",
                    "Trusty Consulting S.r.l. (IT)",
                    "Trusty Consulting GmbH (EN)",
                    "Trusty Consulting GmbH (RM)"
                ),
                new DemoBusinessPartner.DemoAddress("Geschäftsstraße 13", "Demohausen", "1111", "CH", "Democanton"),
                new DemoBusinessPartner.DemoContact(
                    "erika",
                    "müller",
                    "erika.mueller@trusty-consulting.local",
                    "+41548884440",
                    DemoBusinessPartner.DemoContact.Language.DE
                ),
                DemoBusinessPartner.DemoSigningRule.SINGLE_SIGNATURE,
                List.of(
                    new DemoBusinessPartner.DemoSignatory(
                        "Erika",
                        "Müller",
                        "+41776665544",
                        "erika.mueller@trusty-consulting.local"
                    )
                ),
                null,
                "+41791234567",
                null,
                null,
                List.of(
                    new DemoBusinessPartner.DemoTrustOnboarding(
                        // Keep in sync with
                        // ch.admin.bj.swiyu.core.business.common.demodata.DemoDataConstants.TrustOnboardingSubmission#ID_SUCCEEDED
                        UUID.fromString("8369160f-697c-4b12-80d3-91abff1a29ee"),
                        DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingSubmissionStatus.SUCCEEDED,
                        List.of(
                            new DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingSubmissionDocument(
                                "Declaration of intent.pdf",
                                DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingSubmissionDocument.DemoTrustOnboardingSubmissionDocumentTypeDto.TRUST_ONBOARDING_DECLARATION_OF_INTENT,
                                "something"
                            ),
                            new DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingSubmissionDocument(
                                "Handelsregister.pdf",
                                DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingSubmissionDocument.DemoTrustOnboardingSubmissionDocumentTypeDto.TRUST_ONBOARDING_OTHER,
                                "something with UID"
                            ),
                            new DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingSubmissionDocument(
                                "Vertrag_Kaufvertrag_Musterfirma_AG_2025.pdf",
                                DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingSubmissionDocument.DemoTrustOnboardingSubmissionDocumentTypeDto.TRUST_ONBOARDING_OTHER,
                                "something with UID"
                            ),
                            new DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingSubmissionDocument(
                                "Dienstleistungsvertrag_ProjektX_KundeABC_07-11-2025.pdf",
                                DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingSubmissionDocument.DemoTrustOnboardingSubmissionDocumentTypeDto.TRUST_ONBOARDING_OTHER,
                                "something with UID"
                            ),
                            new DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingSubmissionDocument(
                                "Rahmenvertrag_LieferantXYZ_Version2.0.pdf",
                                DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingSubmissionDocument.DemoTrustOnboardingSubmissionDocumentTypeDto.TRUST_ONBOARDING_OTHER,
                                "something with UID"
                            ),
                            new DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingSubmissionDocument(
                                "Mietvertrag_Bürofläche_Zürich_Unterschrieben.pdf",
                                DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingSubmissionDocument.DemoTrustOnboardingSubmissionDocumentTypeDto.TRUST_ONBOARDING_OTHER,
                                "something with UID"
                            ),
                            new DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingSubmissionDocument(
                                "Kooperationsvertrag_Partnerfirma_Gültig_ab_01-01-2026.pdf",
                                DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingSubmissionDocument.DemoTrustOnboardingSubmissionDocumentTypeDto.TRUST_ONBOARDING_OTHER,
                                "something with UID"
                            ),
                            new DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingSubmissionDocument(
                                "Arbeitsvertrag_Max_Muster_Unterschrift_2025.pdf",
                                DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingSubmissionDocument.DemoTrustOnboardingSubmissionDocumentTypeDto.TRUST_ONBOARDING_OTHER,
                                "something with UID"
                            ),
                            new DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingSubmissionDocument(
                                "Geheimhaltungsvereinbarung_NDA_KundeGHI_ProjektY.pdf",
                                DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingSubmissionDocument.DemoTrustOnboardingSubmissionDocumentTypeDto.TRUST_ONBOARDING_OTHER,
                                "something with UID"
                            )
                        ),
                        new DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingTask(
                            UUID.fromString("9eeb93cb-3239-49ef-821f-69e573ea971e"),
                            DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingTask.DemoTrustTaskStatus.ACCEPTED,
                            Instant.now().minus(5, ChronoUnit.DAYS),
                            Instant.now().minus(35, ChronoUnit.DAYS)
                        )
                    ),
                    new DemoBusinessPartner.DemoTrustOnboarding(
                        UUID.fromString("3299cd25-8bab-47b7-9d46-f740be76e57e"),
                        DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingSubmissionStatus.SUBMITTED,
                        DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingSubmissionType.PROFILE_CHANGE,
                        List.of(
                            new DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingSubmissionDocument(
                                "Declaration of intent.pdf",
                                DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingSubmissionDocument.DemoTrustOnboardingSubmissionDocumentTypeDto.TRUST_ONBOARDING_DECLARATION_OF_INTENT,
                                "something else"
                            )
                        ),
                        new DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingTask(
                            UUID.fromString("6470405b-adc5-4720-86ef-c2e9f90d81aa"),
                            DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingTask.DemoTrustTaskStatus.OPENED,
                            Instant.now().plus(1000, ChronoUnit.DAYS),
                            Instant.now().minus(1, ChronoUnit.DAYS)
                        )
                    )
                ),
                DemoBusinessPartner.DemoBusinessPartnerIdentity.of(
                    DemoBusinessPartner.DemoBusinessPartnerIdentity.DemoBusinessPartnerIdentityStatus.ACTIVE,
                    List.of(),
                    List.of()
                )
            )
        ),
        /**
         * Status:<br/>
         * identifier registry: <font color="green">onboarded</font><br/>
         * trust registry: <font color="orange">ongoing</font><br/>
         * <!-- Should be one of: -->
         * <!-- <font color="green">onboarded</font> -->
         * <!-- <font color="orange">ongoing</font> -->
         * <!-- <font color="red">NOT onboarded</font> -->
         * <p>
         * Scenario:<br/>
         * A Governmental BP which already send a Trust Onboarding submission to TMS.<br/>
         * TMS responded with a request for more information.<br/>
         */
        BP_GOV_TRUST_ONBOARDING_MORE_INFO(
            DemoBusinessPartner.of(
                DemoBusinessPartner.DemoBusinessPartnerType.GOVERNMENTAL_INSTITUTION,
                "39f92e48-619e-4e92-8958-468ae138d8a3",
                "p.keller@schleppende-logistik.local",
                fromLanguages("Demo Kanton", "Demo Kanton", "Demo Canton", "Demo Cantone", "Demo Canton", "Demochaun"),
                new DemoBusinessPartner.DemoAddress("Erfolgsstrasse 1", "Demohausen", "1111", "CH", "Democanton"),
                new DemoBusinessPartner.DemoContact(
                    "Sandra",
                    "Schmid",
                    "s.schmid@democanton.admin.local",
                    "+41216548497",
                    DemoBusinessPartner.DemoContact.Language.DE
                ),
                DemoBusinessPartner.DemoSigningRule.JOINT_SIGNATURE_THREE,
                List.of(
                    new DemoBusinessPartner.DemoSignatory(
                        "Sandra",
                        "Schmid",
                        "+41665554433",
                        "s.schmid@democanton.admin.local"
                    ),
                    new DemoBusinessPartner.DemoSignatory(
                        "John",
                        "Doe",
                        "+41776665544",
                        "j.doe@democanton.admin.local"
                    ),
                    new DemoBusinessPartner.DemoSignatory(
                        "Erika",
                        "Müller",
                        "+41554443322",
                        "e.mueller@democanton.admin.local"
                    )
                ),
                null,
                null,
                null,
                null,
                List.of(
                    new DemoBusinessPartner.DemoTrustOnboarding(
                        UUID.fromString("dc828a98-ffb1-4ae4-8f07-b35d2818ac87"),
                        DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingSubmissionStatus.INFORMATION_REQUESTED,
                        List.of(
                            new DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingSubmissionDocument(
                                "Declaration of intent.pdf",
                                DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingSubmissionDocument.DemoTrustOnboardingSubmissionDocumentTypeDto.TRUST_ONBOARDING_DECLARATION_OF_INTENT,
                                "something"
                            )
                        ),
                        new DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingTask(
                            UUID.fromString("37230794-bd9a-4ee5-890c-2c86ba7191ab"),
                            DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingTask.DemoTrustTaskStatus.INFORMATION_REQUESTED,
                            Instant.now().plus(29, ChronoUnit.DAYS),
                            Instant.now().minus(1, ChronoUnit.DAYS)
                        )
                    )
                ),
                null
            )
        ),
        /**
         * Status:<br/>
         * identifier registry: <font color="green">onboarded</font><br/>
         * trust registry: <font color="red">NOT onboarded</font><br/>
         * <!-- Should be one of: -->
         * <!-- <font color="green">onboarded</font> -->
         * <!-- <font color="orange">ongoing</font> -->
         * <!-- <font color="red">NOT onboarded</font> -->
         * <p>
         * Scenario:<br/>
         * BP is created, no interaction otherwise.<br/>
         */
        BUSINESS_BP_BASE_ONBOARDING_ONLY(
            DemoBusinessPartner.of(
                DemoBusinessPartner.DemoBusinessPartnerType.BUSINESS,
                "e97e84e6-f40e-47ba-bdfe-d92f3d3dbc84",
                "helvetica@demo-comp.local",
                fromLanguages(
                    "Demo Unternehmen",
                    "Demo Unternehmen",
                    "Démonstration Entreprise",
                    "Demo Azienda",
                    "Demo Company",
                    "Demo Unternehmen"
                ),
                new DemoBusinessPartner.DemoAddress("Geschäftsstraße 19", "Demohausen", "1111", "CH", "Democanton"),
                new DemoBusinessPartner.DemoContact(
                    "Erika",
                    "Muster",
                    "helvetica@demo-comp.local",
                    "+41791234567",
                    DemoBusinessPartner.DemoContact.Language.DE
                ),
                null,
                null,
                null,
                "+41791234567",
                null,
                null,
                List.of(),
                null
            )
        ),
        /**
         * Status:<br/>
         * identifier registry: <font color="green">onboarded</font><br/>
         * trust registry: <font color="orange">ongoing</font><br/>
         * <!-- Should be one of: -->
         * <!-- <font color="green">onboarded</font> -->
         * <!-- <font color="orange">ongoing</font> -->
         * <!-- <font color="red">NOT onboarded</font> -->
         * <p>
         * Scenario:<br/>
         * BP did send a Trust Onboarding submission which is overdue to be processed on TMS side.<br/>
         */
        BP_TRUST_ONBOARDING_OVERDUE(
            DemoBusinessPartner.of(
                DemoBusinessPartner.DemoBusinessPartnerType.BUSINESS,
                "4b9f08ac-aa29-4bcf-97a4-88e73e49c3e1",
                "p.keller@schleppende-logistik.local",
                fromLanguages(
                    "Schleppende Logistik AG",
                    "Schleppende Logistik AG",
                    "Logistique Lente SA",
                    "Logistica Lenta S.r.l.",
                    "Sluggish Logistics AG",
                    "S.L. AG"
                ),
                new DemoBusinessPartner.DemoAddress("Lagerstrasse 27", "Demohausen", "1111", "CH", "Democanton"),
                new DemoBusinessPartner.DemoContact(
                    "Peter",
                    "Keller",
                    "p.keller@schleppende-logistik.local",
                    "+41548884442",
                    DemoBusinessPartner.DemoContact.Language.DE
                ),
                DemoBusinessPartner.DemoSigningRule.SINGLE_SIGNATURE,
                List.of(
                    new DemoBusinessPartner.DemoSignatory(
                        "Peter",
                        "Keller",
                        "+41776665546",
                        "p.keller@schleppende-logistik.local"
                    )
                ),
                null,
                null,
                null,
                null,
                List.of(
                    new DemoBusinessPartner.DemoTrustOnboarding(
                        UUID.fromString("161d56d8-0999-46e5-a618-ba922414382a"),
                        DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingSubmissionStatus.SUBMITTED,
                        List.of(
                            new DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingSubmissionDocument(
                                "Declaration of intent.pdf",
                                DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingSubmissionDocument.DemoTrustOnboardingSubmissionDocumentTypeDto.TRUST_ONBOARDING_DECLARATION_OF_INTENT,
                                "something overdue"
                            )
                        ),
                        new DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingTask(
                            UUID.fromString("2af84cba-1e20-4251-bc78-30bf358cc18d"),
                            DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingTask.DemoTrustTaskStatus.OPENED,
                            Instant.now().minus(3, ChronoUnit.DAYS),
                            Instant.now().minus(10, ChronoUnit.DAYS)
                        )
                    )
                ),
                null
            )
        ),
        /**
         * Status:<br/>
         * identifier registry: <font color="green">onboarded</font><br/>
         * trust registry: <font color="red">NOT onboarded</font><br/>
         * <!-- Should be one of: -->
         * <!-- <font color="green">onboarded</font> -->
         * <!-- <font color="orange">ongoing</font> -->
         * <!-- <font color="red">NOT onboarded</font> -->
         * <p>
         * Scenario:<br/>
         * BP already send a Trust Onboarding submission which is rejected.<br/>
         * BP prepares a new Trust Onboarding submission which is ready to be submitted.<br/>
         */
        BP_WANTS_TO_BE_TRUSTED(
            DemoBusinessPartner.of(
                DemoBusinessPartner.DemoBusinessPartnerType.BUSINESS,
                "897edd6b-2e3e-4cc2-95a8-5b759c301df8",
                "ceo@m-m.local",
                fromLanguages(
                    "Böswilliges Umzugsunternehmen GmbH",
                    "Böswilliges Umzugsunternehmen GmbH",
                    "Déménageurs malveillants GmbH",
                    "Traslocatori malintenzionati S.r.l.",
                    "Malicious Movers GmbH",
                    "M. M. GmbH"
                ),
                new DemoBusinessPartner.DemoAddress(
                    "Glitterallee 42",
                    "Demohausen",
                    "1111",
                    "Steueroase",
                    "Democanton"
                ),
                new DemoBusinessPartner.DemoContact(
                    "John",
                    "Doe",
                    "ceo@m-m.local",
                    "+41548884441",
                    DemoBusinessPartner.DemoContact.Language.DE
                ),
                DemoBusinessPartner.DemoSigningRule.SINGLE_SIGNATURE,
                List.of(new DemoBusinessPartner.DemoSignatory("John", "Doe", "+41548884441", "ceo@m-m.local")),
                null,
                null,
                List.of("f66469be-fb56-4ed3-be31-a2f5bd670ac9", "ff8757d8-9de8-4cde-a538-1e0e6fc73e5e"),
                null,
                List.of(
                    new DemoBusinessPartner.DemoTrustOnboarding(
                        UUID.fromString("913a09b4-6f6b-4703-a682-1046ccb26abb"),
                        DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingSubmissionStatus.REJECTED,
                        List.of(
                            new DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingSubmissionDocument(
                                "Declaration of intent.pdf",
                                DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingSubmissionDocument.DemoTrustOnboardingSubmissionDocumentTypeDto.TRUST_ONBOARDING_DECLARATION_OF_INTENT,
                                "something"
                            )
                        ),
                        new DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingTask(
                            UUID.fromString("a4a92559-21cc-4ed0-8053-d3c78bb5b5cd"),
                            DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingTask.DemoTrustTaskStatus.REJECTED,
                            Instant.now().plus(1000, ChronoUnit.DAYS),
                            Instant.now().minus(1, ChronoUnit.DAYS)
                        )
                    ),
                    new DemoBusinessPartner.DemoTrustOnboarding(
                        // Keep in sync with
                        // ch.admin.bj.swiyu.core.business.common.demodata.DemoDataConstants.TrustOnboardingSubmission#ID_UNSUBMITTED
                        UUID.fromString("46ada91a-84ce-422b-b9b5-e0d2e3e8c46d"),
                        DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingSubmissionStatus.UNSUBMITTED,
                        List.of(
                            new DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingSubmissionDocument(
                                "Declaration of intent.pdf",
                                DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingSubmissionDocument.DemoTrustOnboardingSubmissionDocumentTypeDto.TRUST_ONBOARDING_DECLARATION_OF_INTENT,
                                "something"
                            )
                        ),
                        null // Onboarding not submitted to TMS
                    )
                ),
                null
            )
        ),
        /**
         * Status:<br/>
         * identifier registry: <font color="green">onboarded</font><br/>
         * trust registry: <font color="orange">ongoing</font><br/>
         * <!-- Should be one of: -->
         * <!-- <font color="green">onboarded</font> -->
         * <!-- <font color="orange">ongoing</font> -->
         * <!-- <font color="red">NOT onboarded</font> -->
         * <p>
         * Scenario:<br/>
         * A Business BP which already sent a Trust Onboarding submission to TMS.<br/>
         * TMS responded with a request for more information (resubmitRequiredUntil set).<br/>
         * Assigned to demo user "doris default".
         */
        BP_BUSINESS_TRUST_ONBOARDING_MORE_INFO(
            DemoBusinessPartner.of(
                DemoBusinessPartner.DemoBusinessPartnerType.BUSINESS,
                "a1b2c3d4-1111-4222-8333-444455556666",
                "info@more-info-demo.local",
                fromLanguages(
                    "More Info Demo GmbH",
                    "More Info Demo GmbH (DE)",
                    "Plus d'Infos Démo GmbH (FR)",
                    "Più Info Demo S.r.l. (IT)",
                    "More Info Demo GmbH (EN)",
                    "More Info Demo GmbH (RM)"
                ),
                new DemoBusinessPartner.DemoAddress(
                    "Informationsstrasse 7",
                    "Demohausen",
                    "1111",
                    "Schweiz",
                    "Democanton"
                ),
                new DemoBusinessPartner.DemoContact(
                    "Doris",
                    "Default",
                    "doris.default@demo.local",
                    "+41548884443",
                    DemoBusinessPartner.DemoContact.Language.DE
                ),
                DemoBusinessPartner.DemoSigningRule.SINGLE_SIGNATURE,
                List.of(
                    new DemoBusinessPartner.DemoSignatory(
                        "Doris",
                        "Default",
                        "+41776665547",
                        "doris.default@demo.local"
                    )
                ),
                "CHE-123.456.789",
                "+41791234567",
                null,
                null,
                List.of(
                    new DemoBusinessPartner.DemoTrustOnboarding(
                        UUID.fromString("b2c3d4e5-2222-4333-8444-555566667777"),
                        DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingSubmissionStatus.INFORMATION_REQUESTED,
                        List.of(
                            new DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingSubmissionDocument(
                                "Declaration of intent.pdf",
                                DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingSubmissionDocument.DemoTrustOnboardingSubmissionDocumentTypeDto.TRUST_ONBOARDING_DECLARATION_OF_INTENT,
                                "something"
                            )
                        ),
                        new DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingTask(
                            UUID.fromString("c3d4e5f6-3333-4444-8555-666677778888"),
                            DemoBusinessPartner.DemoTrustOnboarding.DemoTrustOnboardingTask.DemoTrustTaskStatus.INFORMATION_REQUESTED,
                            Instant.now().plus(29, ChronoUnit.DAYS),
                            Instant.now().minus(1, ChronoUnit.DAYS)
                        )
                    )
                ),
                null
            )
        );

        public final DemoBusinessPartner bp;
    }

    public record DemoBusinessPartner(
        @NotNull DemoBusinessPartnerType type,
        @NotNull String id_s,
        @NotNull UUID id,
        String email,
        @NotNull Map<@NotBlank String, @NotBlank String> names,
        DemoAddress address,
        DemoContact contact,
        DemoSigningRule signatoryRule,
        List<@NotNull DemoSignatory> signatory,
        String uid,
        String contactPhone,
        List<@NotNull DemoIdentifier> identifiers,
        @NotNull List<@NotNull DemoTrustOnboarding> trustOnboardings,
        DemoBusinessPartnerIdentity bpi,
        @NotNull List<@NotNull DemoProtectedVerificationSubmission> protectedVerificationSubmissions
    ) {
        static DemoBusinessPartner of(
            @NotNull DemoBusinessPartnerType type,
            @NotNull String id,
            String email,
            @NotNull Map<@NotBlank String, @NotBlank String> names,
            DemoAddress address,
            DemoContact contact,
            DemoSigningRule signatoryRule,
            List<@NotNull DemoSignatory> signatory,
            String uid,
            String contactPhone,
            List<@NotNull String> exampleIdentifiers,
            List<@NotNull DemoIdentifier> otherIdentifiers,
            @NotNull List<@NotNull DemoTrustOnboarding> trustOnboardings,
            DemoBusinessPartnerIdentity bpi
        ) {
            return of(
                type,
                id,
                email,
                names,
                address,
                contact,
                signatoryRule,
                signatory,
                uid,
                contactPhone,
                exampleIdentifiers,
                otherIdentifiers,
                trustOnboardings,
                bpi,
                List.of()
            );
        }

        static DemoBusinessPartner of(
            @NotNull DemoBusinessPartnerType type,
            @NotNull String id,
            String email,
            @NotNull Map<@NotBlank String, @NotBlank String> names,
            DemoAddress address,
            DemoContact contact,
            DemoSigningRule signatoryRule,
            List<@NotNull DemoSignatory> signatory,
            String uid,
            String contactPhone,
            List<@NotNull String> exampleIdentifiers,
            List<@NotNull DemoIdentifier> otherIdentifiers,
            @NotNull List<@NotNull DemoTrustOnboarding> trustOnboardings,
            DemoBusinessPartnerIdentity bpi,
            @NotNull List<@NotNull DemoProtectedVerificationSubmission> protectedVerificationSubmissions
        ) {
            return new DemoBusinessPartner(
                type,
                id,
                UUID.fromString(id),
                email,
                names,
                address,
                contact,
                signatoryRule,
                signatory,
                uid,
                contactPhone,
                Stream.concat(
                    Stream.concat(
                        // Every BP gets a did:example assigned
                        Stream.of(
                            new DemoIdentifier(
                                UUID.fromString(id),
                                "did:example:" + id + "-" + id,
                                "Example DID",
                                null,
                                bpi != null && (otherIdentifiers == null || otherIdentifiers.isEmpty())
                            )
                        ),
                        // Example DIDs
                        Optional.ofNullable(exampleIdentifiers)
                            .orElseGet(Collections::emptyList)
                            .stream()
                            .map(didSpaceId ->
                                new DemoIdentifier(
                                    UUID.fromString(didSpaceId),
                                    "did:example:" + didSpaceId + "-" + id,
                                    "Example DID",
                                    null,
                                    false
                                )
                            )
                    ),
                    // Some BPs have extra / realistic DIDs
                    Optional.ofNullable(otherIdentifiers).orElseGet(Collections::emptyList).stream()
                ).toList(),
                trustOnboardings,
                bpi,
                protectedVerificationSubmissions
            );
        }

        public enum DemoBusinessPartnerType {
            GOVERNMENTAL_INSTITUTION,
            BUSINESS,
            INDIVIDUAL,
        }

        public enum DemoSigningRule {
            SINGLE_SIGNATURE,
            JOINT_SIGNATURE_TWO,
            JOINT_SIGNATURE_THREE,
        }

        public record DemoContact(
            String firstName,
            String lastName,
            String email,
            String phone,
            Language correspondingLanguage
        ) {
            public enum Language {
                EN,
                DE,
                FR,
                IT,
                RM,
            }
        }

        public record DemoAddress(String street, String city, String postalCode, String country, String region) {}

        public record DemoSignatory(String firstName, String lastName, String phone, String email) {}

        public record DemoIdentifier(UUID id, String did, String description, String data, boolean isTrustOnboarded) {}

        public record DemoTrustOnboarding(
            @NotNull UUID submissionId,
            @NotNull DemoTrustOnboardingSubmissionStatus status,
            @NotNull DemoTrustOnboardingSubmissionType submissionType,
            @NotNull List<DemoTrustOnboardingSubmissionDocument> documents,
            DemoTrustOnboardingTask task
        ) {
            /**
             * Convenience constructor: an onboarding is a REGISTRATION unless a specific submission
             * type (e.g. PROFILE_CHANGE) is given.
             */
            public DemoTrustOnboarding(
                UUID submissionId,
                DemoTrustOnboardingSubmissionStatus status,
                List<DemoTrustOnboardingSubmissionDocument> documents,
                DemoTrustOnboardingTask task
            ) {
                this(submissionId, status, DemoTrustOnboardingSubmissionType.REGISTRATION, documents, task);
            }

            public enum DemoTrustOnboardingSubmissionType {
                REGISTRATION,
                PROFILE_CHANGE,
                RENEWAL,
            }

            public enum DemoTrustOnboardingSubmissionStatus {
                UNSUBMITTED, // user can update at any time
                UNSUBMITTED_TIMEOUT,
                SUBMITTED,
                SUCCEEDED, // approved and statements for all DIDs are published in trust registry
                REJECTED,
                INFORMATION_REQUESTED,
            }

            public record DemoTrustOnboardingSubmissionDocument(
                String fileName,
                DemoTrustOnboardingSubmissionDocumentTypeDto type,
                String content
            ) {
                public enum DemoTrustOnboardingSubmissionDocumentTypeDto {
                    TRUST_ONBOARDING_OTHER,
                    TRUST_ONBOARDING_DECLARATION_OF_INTENT,
                }
            }

            public record DemoTrustOnboardingTask(
                @NotNull UUID id,
                @NotNull DemoTrustTaskStatus status,
                @NotNull Instant dueAt,
                @NotNull Instant submittedAt
            ) {
                public enum DemoTrustTaskStatus {
                    REJECTED,
                    ACCEPTED,
                    OPENED,
                    INFORMATION_REQUESTED,
                    RESUBMITTED,
                }
            }
        }

        /**
         * Models both sides of a protected verification request together, the same way {@link DemoTrustOnboarding}
         * models a trust onboarding submission alongside its task: CBS reads the outer fields to seed a
         * {@code ProtectedVerificationSubmission}, TMS reads the nested {@code task} to seed the matching
         * {@code ProtectedVerificationRequestTask}. Keep this in sync with an authorization granted on
         * {@link DemoBusinessPartnerIdentity#protectedVerificationAuthorizations()} - an authorization can only
         * ever be created by approving a request like this one, never on its own.
         */
        public record DemoProtectedVerificationSubmission(
            @NotNull UUID submissionId,
            @NotNull UUID sbnId,
            @NotNull DemoProtectedVerificationCategory category,
            @NotNull DemoProtectedVerificationSubmissionStatus status,
            @NotNull String reason,
            DemoProtectedVerificationRequestTask task
        ) {
            public enum DemoProtectedVerificationCategory {
                PERSONAL_ADMINISTRATIVE_NUMBER,
            }

            public enum DemoProtectedVerificationSubmissionStatus {
                SUBMITTED,
                APPROVED,
                REJECTED,
            }

            public record DemoProtectedVerificationRequestTask(
                @NotNull DemoProtectedVerificationTaskStatus status,
                @NotNull Instant dueAt,
                @NotNull Instant submittedAt
            ) {
                public enum DemoProtectedVerificationTaskStatus {
                    OPENED,
                    ACCEPTED,
                    REJECTED,
                }
            }
        }

        public record DemoBusinessPartnerIdentity(
            @NotNull DemoBusinessPartnerIdentityStatus status,
            @NotNull Instant validUntil,
            @NotNull List<@NotNull DemoProtectedVerificationAuthorization> protectedVerificationAuthorizations,
            @NotNull List<@NotNull DemoProtectedIssuanceAuthorization> protectedIssuanceAuthorizations
        ) {
            public static DemoBusinessPartnerIdentity of(
                @NotNull DemoBusinessPartnerIdentityStatus status,
                @NotNull List<@NotNull DemoProtectedVerificationAuthorization> protectedVerificationAuthorizations,
                @NotNull List<@NotNull DemoProtectedIssuanceAuthorization> protectedIssuanceAuthorizations
            ) {
                return new DemoBusinessPartnerIdentity(
                    status,
                    Instant.now().atZone(ZoneId.of("Europe/Zurich")).plusYears(3).toInstant(),
                    protectedVerificationAuthorizations,
                    protectedIssuanceAuthorizations
                );
            }

            public enum DemoBusinessPartnerIdentityStatus {
                DEACTIVATED,
                ACTIVE,
            }

            public record DemoProtectedVerificationAuthorization(UUID id, DemoProtectedVerificationField field) {
                public enum DemoProtectedVerificationField {
                    AHV_NUMBER,
                }
            }

            public record DemoProtectedIssuanceAuthorization(
                UUID id,
                String identifier,
                @NotNull Map<@NotBlank String, @NotBlank String> reason,
                // For ease of use currently the ProtectedIssuanceEntry are part of this config
                UUID protectedVctEntryId,
                String vct,
                @NotNull Map<@NotBlank String, @NotBlank String> vctName
            ) {}
        }
    }
}
