package ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding;

import static ch.admin.bj.swiyu.core.business.test.BusinessEntityTestData.DEFAULT_ENTITY;
import static ch.admin.bj.swiyu.core.business.test.TrustOnboardingSubmissionTestData.trustOnboardingSubmission;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

import ch.admin.bj.swiyu.core.business.common.domain.Address;
import ch.admin.bj.swiyu.core.business.common.domain.BusinessPartnerType;
import ch.admin.bj.swiyu.core.business.common.domain.Contact;
import ch.admin.bj.swiyu.core.business.common.domain.Language;
import ch.admin.bj.swiyu.core.business.common.exceptions.InternalStorageException;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.ProofOfPossession;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.Signatory;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.SigningRule;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustOnboardingSubmission;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustOnboardingSubmissionDomainService;
import ch.admin.bj.swiyu.core.business.modules.trust.exceptions.DeclarationOfIntentPdfGenerationException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrustDeclarationOfIntentPdfServiceTest {

    private static final String FIELD_UID_TEXTBOX = "uid_textbox";
    private static final String FIELD_NAME_TEXTBOX = "name_textbox";
    private static final String FIELD_ADDRESS_TEXTBOX = "address_textbox";
    private static final String FIELD_DATE_TEXTBOX = "date_textbox";
    private static final String FIELD_DIDS_TEXTBOX = "dids_textbox";

    @Mock
    private TrustOnboardingSubmissionDomainService trustOnboardingSubmissionService;

    @InjectMocks
    private TrustDeclarationOfIntentPdfService trustDeclarationOfIntentPdfService;

    private UUID submissionId;

    @BeforeEach
    void setUp() {
        submissionId = UUID.randomUUID();
    }

    @Test
    void streamFilledDeclarationOfIntentPdf_fillsAllFormFieldsForBusinessSingleSignatureGermanTemplate()
        throws IOException {
        var submission = businessSubmission(SigningRule.SINGLE_SIGNATURE, List.of(signatory("John", "Doe")));
        when(trustOnboardingSubmissionService.getTrustOnboardingSubmission(submissionId)).thenReturn(submission);

        var pdfBytes = generatePdfBytes(submissionId, Language.DE);

        assertThat(pdfBytes).isNotEmpty();

        try (var pdf = Loader.loadPDF(pdfBytes)) {
            var form = pdf.getDocumentCatalog().getAcroForm();
            assertThat(form).isNotNull();

            assertThat(fieldValue(form, FIELD_UID_TEXTBOX)).isEqualTo("CHE-123.456.789");
            assertThat(fieldValue(form, FIELD_NAME_TEXTBOX)).isEqualTo("Test Entity Name DE");
            assertThat(fieldValue(form, FIELD_ADDRESS_TEXTBOX)).isEqualTo("Test Street, 1234 Test City, CH");
            assertThat(fieldValue(form, FIELD_DATE_TEXTBOX)).isEqualTo(expectedDateHeader(Language.DE));
            assertThat(fieldValue(form, FIELD_DIDS_TEXTBOX)).isEqualTo("did:example:123\ndid:example:abc");
            assertThat(fieldValue(form, "signatory_1_name_textbox")).isEqualTo("John Doe");

            assertThat(form.getField(FIELD_UID_TEXTBOX).isReadOnly()).isTrue();
            assertThat(form.getField(FIELD_NAME_TEXTBOX).isReadOnly()).isTrue();
        }
    }

    @Test
    void streamFilledDeclarationOfIntentPdf_fillsAllSignatoryFieldsForBusinessJointSignatureTemplate()
        throws IOException {
        var submission = businessSubmission(
            SigningRule.JOINT_SIGNATURE_TWO,
            List.of(signatory("Anna", "Muster"), signatory("Ben", "Beispiel"))
        );
        when(trustOnboardingSubmissionService.getTrustOnboardingSubmission(submissionId)).thenReturn(submission);

        var pdfBytes = generatePdfBytes(submissionId, Language.DE);

        try (var pdf = Loader.loadPDF(pdfBytes)) {
            var form = pdf.getDocumentCatalog().getAcroForm();
            assertThat(fieldValue(form, "signatory_1_name_textbox")).isEqualTo("Anna Muster");
            assertThat(fieldValue(form, "signatory_2_name_textbox")).isEqualTo("Ben Beispiel");
        }
    }

    @SuppressWarnings({ "java:S1874" }) // Remove with EID-6656
    @Test
    void streamFilledDeclarationOfIntentPdf_withUnknownPartnerType_throwsDeclarationOfIntentPdfGenerationException() {
        var submission = submission(BusinessPartnerType.UNKNOWN, SigningRule.SINGLE_SIGNATURE, List.of(), true);
        when(trustOnboardingSubmissionService.getTrustOnboardingSubmission(submissionId)).thenReturn(submission);

        assertThatThrownBy(() -> generatePdfBytes(submissionId, Language.DE))
            .isInstanceOf(DeclarationOfIntentPdfGenerationException.class)
            .hasMessageContaining("Unknown partner type");
    }

    @Test
    void streamFilledDeclarationOfIntentPdf_allSwissLocalesExist() {
        var submission = businessSubmission(SigningRule.SINGLE_SIGNATURE, List.of(signatory("John", "Doe")));
        when(trustOnboardingSubmissionService.getTrustOnboardingSubmission(submissionId)).thenReturn(submission);

        for (var language : Language.values()) {
            assertThatNoException().isThrownBy(() -> generatePdfBytes(submissionId, language));
        }
    }

    @ParameterizedTest
    @MethodSource("individualTemplateNotFoundCases")
    void streamFilledDeclarationOfIntentPdf_withIndividualPartnerType_throwsWhenTemplatePathUsesZeroSignatures(
        Boolean isRegisteredInCommercialRegister,
        String expectedTemplatePath
    ) {
        var submission = submission(
            BusinessPartnerType.INDIVIDUAL,
            SigningRule.SINGLE_SIGNATURE,
            List.of(signatory("John", "Doe")),
            isRegisteredInCommercialRegister
        );
        when(trustOnboardingSubmissionService.getTrustOnboardingSubmission(submissionId)).thenReturn(submission);

        assertThatThrownBy(() -> generatePdfBytes(submissionId, Language.DE))
            .isInstanceOf(InternalStorageException.class)
            .hasMessageContaining(expectedTemplatePath);
    }

    @Test
    void streamFilledDeclarationOfIntentPdf_withMoreSignatoriesThanTemplateFields_throwsDeclarationOfIntentPdfGenerationException() {
        var submission = businessSubmission(
            SigningRule.SINGLE_SIGNATURE,
            List.of(signatory("Anna", "Muster"), signatory("Ben", "Beispiel"))
        );
        when(trustOnboardingSubmissionService.getTrustOnboardingSubmission(submissionId)).thenReturn(submission);

        assertThatThrownBy(() -> generatePdfBytes(submissionId, Language.DE))
            .isInstanceOf(DeclarationOfIntentPdfGenerationException.class)
            .hasMessageContaining("signatory_2_name_textbox");
    }

    @Test
    void streamFilledDeclarationOfIntentPdf_withSingleDid_doesNotAddTrailingNewline() throws IOException {
        var submission = submissionWithProofOfPossessions(
            businessSubmission(SigningRule.SINGLE_SIGNATURE, List.of(signatory("John", "Doe"))),
            List.of(new ProofOfPossession("did:example:single", UUID.randomUUID().toString()))
        );
        when(trustOnboardingSubmissionService.getTrustOnboardingSubmission(submissionId)).thenReturn(submission);

        var pdfBytes = generatePdfBytes(submissionId, Language.DE);

        try (var pdf = Loader.loadPDF(pdfBytes)) {
            assertThat(fieldValue(pdf.getDocumentCatalog().getAcroForm(), FIELD_DIDS_TEXTBOX)).isEqualTo(
                "did:example:single"
            );
        }
    }

    @Test
    void getFieldMapping_withIndividualPrivate_usesFullNameAndOmitsUidField()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        var submission = submission(BusinessPartnerType.INDIVIDUAL, SigningRule.SINGLE_SIGNATURE, List.of(), false);

        var fieldMapping = getFieldMapping(submission, Language.DE);

        assertThat(fieldMapping).containsEntry(FIELD_NAME_TEXTBOX, "John Doe").doesNotContainKey(FIELD_UID_TEXTBOX);
    }

    @ParameterizedTest
    @MethodSource("supportedTemplateConfigurations")
    void streamFilledDeclarationOfIntentPdf_fillsRequiredFieldsForEachTemplate(
        BusinessPartnerType partnerType,
        SigningRule signingRule,
        List<Signatory> signatories,
        Boolean isRegisteredInCommercialRegister,
        Language language,
        String datePrefix,
        String expectedEntityName,
        boolean expectsUidField
    ) throws IOException {
        var submission = submission(partnerType, signingRule, signatories, isRegisteredInCommercialRegister);
        when(trustOnboardingSubmissionService.getTrustOnboardingSubmission(submissionId)).thenReturn(submission);

        var pdfBytes = generatePdfBytes(submissionId, language);

        assertThat(pdfBytes).isNotEmpty();

        try (var pdf = Loader.loadPDF(pdfBytes)) {
            var form = pdf.getDocumentCatalog().getAcroForm();
            assertThat(form).isNotNull();
            assertThat(fieldValue(form, FIELD_NAME_TEXTBOX)).isEqualTo(expectedEntityName);
            assertThat(fieldValue(form, FIELD_ADDRESS_TEXTBOX)).isEqualTo("Test Street, 1234 Test City, CH");
            assertThat(fieldValue(form, FIELD_DATE_TEXTBOX)).isEqualTo(datePrefix + formattedToday(language));
            assertThat(fieldValue(form, FIELD_DIDS_TEXTBOX)).isEqualTo("did:example:123\ndid:example:abc");
            if (expectsUidField) {
                assertThat(fieldValue(form, FIELD_UID_TEXTBOX)).isEqualTo("CHE-123.456.789");
            } else {
                assertThat(form.getField(FIELD_UID_TEXTBOX)).isNull();
            }

            for (var i = 0; i < signatories.size(); i++) {
                assertThat(fieldValue(form, "signatory_%d_name_textbox".formatted(i + 1))).isEqualTo(
                    signatories.get(i).getFullName()
                );
            }
        }
    }

    private byte[] generatePdfBytes(UUID id, Language language) {
        var out = new ByteArrayOutputStream();
        trustDeclarationOfIntentPdfService.streamFilledDeclarationOfIntentPdf(id, language, out);
        return out.toByteArray();
    }

    private static Stream<Arguments> individualTemplateNotFoundCases() {
        return Stream.of(
            Arguments.of(true, "doi-templates/individual-business-0-de-CH.pdf"),
            Arguments.of(false, "doi-templates/individual-private-0-de-CH.pdf")
        );
    }

    private static Stream<Arguments> supportedTemplateConfigurations() {
        return Stream.of(
            Arguments.of(
                BusinessPartnerType.BUSINESS,
                SigningRule.SINGLE_SIGNATURE,
                List.of(signatory("John", "Doe")),
                true,
                Language.DE,
                "Bern, ",
                "Test Entity Name DE",
                true
            ),
            Arguments.of(
                BusinessPartnerType.BUSINESS,
                SigningRule.JOINT_SIGNATURE_TWO,
                List.of(signatory("John", "Doe"), signatory("Jane", "Roe")),
                true,
                Language.FR,
                "Berne, ",
                "Test Entity Name FR",
                true
            ),
            Arguments.of(
                BusinessPartnerType.BUSINESS,
                SigningRule.JOINT_SIGNATURE_THREE,
                List.of(signatory("John", "Doe"), signatory("Jane", "Roe"), signatory("Alex", "Sample")),
                true,
                Language.IT,
                "Berna, ",
                "Test Entity Name IT",
                true
            ),
            Arguments.of(
                BusinessPartnerType.GOVERNMENTAL_INSTITUTION,
                SigningRule.SINGLE_SIGNATURE,
                List.of(signatory("John", "Doe")),
                true,
                Language.EN,
                "Bern, ",
                "Test Entity Name EN",
                true
            ),
            Arguments.of(
                BusinessPartnerType.GOVERNMENTAL_INSTITUTION,
                SigningRule.JOINT_SIGNATURE_THREE,
                List.of(signatory("John", "Doe"), signatory("Jane", "Roe"), signatory("Alex", "Sample")),
                true,
                Language.FR,
                "Berne, ",
                "Test Entity Name FR",
                true
            )
        );
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> getFieldMapping(
        TrustOnboardingSubmission trustOnboardingSubmission,
        Language language
    ) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method getFieldMapping = TrustDeclarationOfIntentPdfService.class.getDeclaredMethod(
            "getFieldMapping",
            TrustOnboardingSubmission.class,
            Language.class
        );
        getFieldMapping.setAccessible(true);
        return (Map<String, String>) getFieldMapping.invoke(null, trustOnboardingSubmission, language);
    }

    private static TrustOnboardingSubmission businessSubmission(SigningRule signingRule, List<Signatory> signatories) {
        return submission(BusinessPartnerType.BUSINESS, signingRule, signatories, true);
    }

    private static TrustOnboardingSubmission submission(
        BusinessPartnerType partnerType,
        SigningRule signingRule,
        List<Signatory> signatories,
        Boolean isRegisteredInCommercialRegister
    ) {
        var base = trustOnboardingSubmission(UUID.randomUUID(), DEFAULT_ENTITY);
        return new TrustOnboardingSubmission(
            base.getId(),
            base.getPartnerId(),
            base.getEntityName(),
            base.getEntityAddress(),
            base.getEntityEmail(),
            contactWithAddress(),
            base.getCorrespondingLanguage(),
            base.getUid(),
            isRegisteredInCommercialRegister,
            base.getProofOfPossessions(),
            partnerType,
            signingRule,
            signatories,
            Instant.now()
        );
    }

    private static TrustOnboardingSubmission submissionWithProofOfPossessions(
        TrustOnboardingSubmission base,
        List<ProofOfPossession> proofOfPossessions
    ) {
        return new TrustOnboardingSubmission(
            base.getId(),
            base.getPartnerId(),
            base.getEntityName(),
            base.getEntityAddress(),
            base.getEntityEmail(),
            base.getContactPerson(),
            base.getCorrespondingLanguage(),
            base.getUid(),
            base.getIsRegisteredInCommercialRegister(),
            proofOfPossessions,
            base.getRequestedPartnerType(),
            base.getSigningRule(),
            base.getSignatories(),
            Instant.now()
        );
    }

    private static Contact contactWithAddress() {
        return Contact.builder()
            .firstName("John")
            .lastName("Doe")
            .email("john.doe@example.com")
            .phone("+41 79 123 45 67")
            .address(Address.builder().street("Test Street").city("Test City").postalCode("1234").country("CH").build())
            .build();
    }

    private static Signatory signatory(String firstName, String lastName) {
        return new Signatory(
            firstName,
            lastName,
            "+41 79 000 00 00",
            "%s.%s@example.com".formatted(firstName, lastName)
        );
    }

    private static String expectedDateHeader(Language language) {
        return (
            switch (language) {
                case DE -> "Bern, ";
                case FR -> "Berne, ";
                case IT, RM -> "Berna, ";
                case EN -> "Bern, ";
            } +
            formattedToday(language)
        );
    }

    private static String formattedToday(Language language) {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy", language.getSwissLocale()));
    }

    private static String fieldValue(PDAcroForm form, String fieldName) {
        return form.getField(fieldName).getValueAsString();
    }
}
