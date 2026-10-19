package ch.admin.bj.swiyu.core.business.test;

import ch.admin.bj.swiyu.core.business.modules.documents.domain.PartnerDocument;
import ch.admin.bj.swiyu.core.business.modules.documents.domain.PartnerDocumentType;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import lombok.experimental.UtilityClass;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

@UtilityClass
public class PartnerDocumentTestData {

    public static MockMultipartFile multipartFile() {
        return new MockMultipartFile(
            "file",
            "doi.pdf",
            "application/pdf",
            "test-content".getBytes(StandardCharsets.UTF_8)
        );
    }

    public static PartnerDocument partnerDocumemt(
        UUID partnerId,
        PartnerDocumentType type,
        UUID trustOnboardingSubmissionId
    ) {
        return PartnerDocument.createTrustOnboardingSubissionPartnerDocument(
            UUID.randomUUID(),
            partnerId,
            type,
            "test-file.pdf",
            MediaType.APPLICATION_PDF,
            "dummy-key",
            trustOnboardingSubmissionId,
            "dummy-virus-scan-id",
            Instant.now()
        );
    }
}
