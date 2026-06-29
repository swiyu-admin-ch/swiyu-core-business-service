package ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding;

import static java.util.Map.entry;

import ch.admin.bj.swiyu.core.business.common.domain.BusinessPartnerType;
import ch.admin.bj.swiyu.core.business.common.domain.Language;
import ch.admin.bj.swiyu.core.business.common.exceptions.InternalStorageException;
import ch.admin.bj.swiyu.core.business.common.service.LocalizedMapUtil;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.ProofOfPossession;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustOnboardingSubmission;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustOnboardingSubmissionDomainService;
import ch.admin.bj.swiyu.core.business.modules.trust.exceptions.DeclarationOfIntentPdfGenerationException;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrustDeclarationOfIntentPdfService {

    private static final String DOI_TEMPLATES_RESOURCE_FILE_TEMPLATE = "doi-templates/%s-%d-%s.pdf";
    private static final String DOI_TEMPLATES_BUSINESS_PREFIX = "business";
    private static final String DOI_TEMPLATES_GOVERNMENT_PREFIX = "government";
    private static final String DOI_TEMPLATES_INDIVIDUAL_BUSINESS_PREFIX = "individual-business";
    private static final String DOI_TEMPLATES_INDIVIDUAL_PRIVATE_PREFIX = "individual-private";

    @SuppressWarnings("java:S1192")
    private static final Map<String, String> DOI_DATE_HEADER_PREFIX = LocalizedMapUtil.fromLanguages(
        "Bern, ",
        "Bern, ",
        "Berne, ",
        "Berna, ",
        "Bern, ",
        "Berna, "
    );

    private static final String FIELD_UID_TEXTBOX = "uid_textbox";
    private static final String FIELD_NAME_TEXTBOX = "name_textbox";
    private static final String FIELD_ADDRESS_TEXTBOX = "address_textbox";
    private static final String FIELD_DATE_TEXTBOX = "date_textbox";
    private static final String FIELD_DIDS_TEXTBOX = "dids_textbox";
    private static final String FIELD_SIGNATORY_NAME_TEXTBOX_TEMPLATE = "signatory_%d_name_textbox";

    private final TrustOnboardingSubmissionDomainService trustOnboardingSubmissionService;

    private static PDAcroForm getPdAcroForm(PDDocument pdf) {
        var catalog = pdf.getDocumentCatalog();
        if (catalog == null) {
            throw new DeclarationOfIntentPdfGenerationException(
                "Declaration of intent PDF template has no document catalog",
                null
            );
        }
        var form = catalog.getAcroForm();
        if (form == null) {
            throw new DeclarationOfIntentPdfGenerationException(
                "Declaration of intent PDF template has no AcroForm",
                null
            );
        }
        return form;
    }

    private static Map<String, String> getFieldMapping(
        TrustOnboardingSubmission trustOnboardingSubmission,
        Language language
    ) {
        var fieldMapping = new HashMap<>(
            Map.ofEntries(
                entry(FIELD_ADDRESS_TEXTBOX, trustOnboardingSubmission.getEntityAddress().getFullAddressOneLine()),
                entry(FIELD_DATE_TEXTBOX, getDateHeader(language)),
                entry(FIELD_DIDS_TEXTBOX, getDidText(trustOnboardingSubmission))
            )
        );

        if (
            trustOnboardingSubmission.getRequestedPartnerType() != BusinessPartnerType.INDIVIDUAL ||
            Boolean.TRUE.equals(trustOnboardingSubmission.getIsRegisteredInCommercialRegister())
        ) {
            fieldMapping.put(FIELD_UID_TEXTBOX, trustOnboardingSubmission.getUid());
            fieldMapping.put(
                FIELD_NAME_TEXTBOX,
                LocalizedMapUtil.getByLanguageOrDefault(trustOnboardingSubmission.getEntityName(), language)
            );
        } else {
            fieldMapping.put(FIELD_NAME_TEXTBOX, trustOnboardingSubmission.getContactPerson().getFullName());
        }

        var signatories = trustOnboardingSubmission.getSignatories();
        for (var i = 0; i < signatories.size(); i++) {
            fieldMapping.put(FIELD_SIGNATORY_NAME_TEXTBOX_TEMPLATE.formatted(i + 1), signatories.get(i).getFullName());
        }

        return fieldMapping;
    }

    private static String getDidText(TrustOnboardingSubmission trustOnboardingSubmission) {
        return trustOnboardingSubmission
            .getProofOfPossessions()
            .stream()
            .map(ProofOfPossession::getDid)
            .collect(Collectors.joining("\n"));
    }

    private static String getDateHeader(Language language) {
        var prefix = LocalizedMapUtil.getByLanguageOrDefault(DOI_DATE_HEADER_PREFIX, language);

        var today = LocalDate.now();
        var formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", language.getSwissLocale());
        return prefix + today.format(formatter);
    }

    private static String getCorrectPdfFilename(
        TrustOnboardingSubmission trustOnboardingSubmission,
        Language language
    ) {
        var prefix = switch (trustOnboardingSubmission.getRequestedPartnerType()) {
            case BUSINESS -> DOI_TEMPLATES_BUSINESS_PREFIX;
            case GOVERNMENTAL_INSTITUTION -> DOI_TEMPLATES_GOVERNMENT_PREFIX;
            case INDIVIDUAL -> Boolean.TRUE.equals(trustOnboardingSubmission.getIsRegisteredInCommercialRegister())
                ? DOI_TEMPLATES_INDIVIDUAL_BUSINESS_PREFIX
                : DOI_TEMPLATES_INDIVIDUAL_PRIVATE_PREFIX;
            case UNKNOWN -> throw new DeclarationOfIntentPdfGenerationException("Unknown partner type");
        };
        var signatureCount = trustOnboardingSubmission
            .getSigningRule()
            .getRequiredSignatories(trustOnboardingSubmission.getRequestedPartnerType());

        return DOI_TEMPLATES_RESOURCE_FILE_TEMPLATE.formatted(
            prefix,
            signatureCount,
            language.getSwissLocale().toLanguageTag()
        );
    }

    // This method streams the filled declaration of intent to the given OutputStream.
    // The code will be used with EID-6148 and might need slight changes.
    // Currently it is used to validate that the DOI-PDFs in the repository have the correct fields and can be filled out.
    public void streamFilledDeclarationOfIntentPdf(
        UUID trustOnboardingSubmissionId,
        Language language,
        OutputStream outputStream
    ) {
        var trustOnboardingSubmission = trustOnboardingSubmissionService.getTrustOnboardingSubmission(
            trustOnboardingSubmissionId
        );
        var filename = getCorrectPdfFilename(trustOnboardingSubmission, language);
        var classLoader = TrustDeclarationOfIntentPdfService.class.getClassLoader();

        try (var inputStream = classLoader.getResourceAsStream(filename)) {
            if (inputStream == null) {
                throw new InternalStorageException("Declaration of intent PDF template not found: " + filename, null);
            }
            var bytes = inputStream.readAllBytes();

            try (var pdf = Loader.loadPDF(bytes)) {
                fillPdf(pdf, trustOnboardingSubmission, language, outputStream);
            }
        } catch (IOException e) {
            throw new InternalStorageException("Error reading declaration of intent PDF template: " + filename, e);
        }
    }

    private void fillPdf(
        PDDocument pdf,
        TrustOnboardingSubmission trustOnboardingSubmission,
        Language language,
        OutputStream outputStream
    ) {
        var form = getPdAcroForm(pdf);

        var fieldMapping = getFieldMapping(trustOnboardingSubmission, language);

        fieldMapping.forEach((fieldName, value) -> {
            var field = form.getField(fieldName);
            if (field == null) {
                throw new DeclarationOfIntentPdfGenerationException(
                    "Declaration of intent PDF template is missing form field: " + fieldName,
                    null
                );
            }

            try {
                field.setValue(value);
            } catch (IOException e) {
                throw new DeclarationOfIntentPdfGenerationException(
                    "Failed to set value on declaration of intent PDF field: " + fieldName,
                    e
                );
            }

            field.setReadOnly(true);
        });

        try {
            pdf.save(outputStream);
        } catch (IOException e) {
            throw new InternalStorageException("Failed to write declaration of intent PDF to response", e);
        }
    }
}
