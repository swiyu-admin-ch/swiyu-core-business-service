package ch.admin.bj.swiyu.core.business.test;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;

/**
 * Test helpers for assertions on generated PDF documents.
 */
public final class PdfTestUtil {

    private PdfTestUtil() {}

    /**
     * Extracts the text of the given PDF and collapses all whitespace runs to a single space, so assertions do
     * not depend on line breaks inserted by the PDF layout.
     */
    public static String extractText(byte[] pdf) throws IOException {
        try (var document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document).replaceAll("\\s+", " ").trim();
        }
    }

    /**
     * Writes the PDF to a temporary file and opens it with the operating system's default viewer. Only intended
     * for disabled local-development tests.
     */
    public static void writeAndOpenPdf(byte[] pdf) throws IOException {
        var path = Files.createTempFile("test", ".pdf");
        Files.write(path, pdf);
        Desktop.getDesktop().open(path.toFile());
    }
}
