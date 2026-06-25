package ch.admin.bj.swiyu.core.business.test;

import java.util.UUID;
import lombok.Builder;
import lombok.experimental.UtilityClass;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

@UtilityClass
public class TrustOnboardingSubmissionDocumentTestData {

    public static MockMultipartFile getTestFile(TestDocument document) {
        var content = new byte[Math.toIntExact(document.dataSize().toBytes())];
        // Optionally fill the array with data
        for (var i = 0; i < document.dataSize().toBytes(); i++) {
            content[i] = 'A'; // or any other byte value
        }
        return new MockMultipartFile("file", document.fileName(), document.contentType(), content);
    }

    @Builder
    public record TestDocument(
        UUID trustOnboardingSubmissionId,
        String fileName,
        String contentType,
        DataSize dataSize,
        /**
         * @see PartnerDocumentTypeDto
         */
        String partnerDocumentType
    ) {
        public static class TestDocumentBuilder {

            // TestDocumentBuilder is used internally to set default values
            TestDocumentBuilder() {
                fileName = "test_document.pdf";
                contentType = "application/pdf";
                dataSize = DataSize.parse("1KB");
                partnerDocumentType = "TRUST_ONBOARDING_OTHER";
            }
        }
    }
}
