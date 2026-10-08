package ch.admin.bj.swiyu.core.business.modules.email.domain;

import static ch.admin.bj.swiyu.core.business.modules.email.domain.EmailTemplateFixture.variablesFor;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Writes every rendered email to {@code target/email-preview} so a human can look at it.
 *
 * <p>Not a check of anything - the assertions only confirm the files were written. It exists because
 * the alternative way to see what the 15 emails look like is to start the application, Kafka, a
 * database and Mailpit and then trigger 15 flows, which nobody does twice. This produces the same
 * pages from {@code ./mvnw test -Dtest=DevHtmlEmailPreviewTest} in a couple of seconds, and it is
 * what the reviewers of the four translations and the sprint review are shown.
 *
 * <p>Two differences to what actually goes out, both deliberate:
 *
 * <ul>
 *   <li>{@code cid:} references are rewritten to the copied image files - a browser cannot resolve a
 *       content id, that only means something inside a MIME message
 *   <li>the plain text alternative is written next to each page, because it is a real part of the
 *       message and gets forgotten otherwise
 * </ul>
 */
class DevHtmlEmailPreviewTest {

    private static final Path PREVIEW_DIR = Path.of("target", "email-preview");
    private static final Path IMAGE_SOURCE_DIR = Path.of("src", "main", "resources", "email-images");

    private static final Pattern CID_REFERENCE = Pattern.compile("src=\"cid:([a-z0-9-]+)\"");

    private final EmailContentRenderer renderer = EmailTemplateFixture.renderer();
    private final HtmlToPlainTextConverter converter = new HtmlToPlainTextConverter();

    @Test
    void writesEveryEmailAsAPageThatCanBeOpenedInABrowser() throws IOException {
        Files.createDirectories(PREVIEW_DIR);
        copyLogos();

        var index = new StringBuilder(
            """
            <!DOCTYPE html>
            <html lang="en">
            <head><meta charset="utf-8"><title>Partner notification emails</title></head>
            <body>
            <h1>Partner notification emails</h1>
            <p>Rendered by DevHtmlEmailPreviewTest with the stage prefix of a test stage.</p>
            <ul>
            """
        );

        for (var emailType : EmailType.values()) {
            var rendered = renderer.render(emailType, variablesFor(emailType), "[DEV]");
            var name = emailType.getTemplateName();

            write(PREVIEW_DIR.resolve(name + ".html"), browserReadable(rendered.body()));
            write(PREVIEW_DIR.resolve(name + ".txt"), converter.convert(rendered.body()));

            index
                .append("<li><a href=\"")
                .append(name)
                .append(".html\">")
                .append(emailType.name())
                .append("</a> — ")
                .append(rendered.subject())
                .append(" (<a href=\"")
                .append(name)
                .append(".txt\">text part</a>)</li>\n");
        }
        write(
            PREVIEW_DIR.resolve("index.html"),
            index
                .append(
                    """
                    </ul>
                    </body>
                    </html>
                    """
                )
                .toString()
        );

        assertThat(PREVIEW_DIR.resolve("index.html")).exists();
        assertThat(Arrays.stream(EmailType.values()).map(type -> PREVIEW_DIR.resolve(type.getTemplateName() + ".html")))
            .isNotEmpty()
            .allSatisfy(page -> assertThat(page).exists());
    }

    /**
     * A browser has no MIME message to resolve a content id against, so the references are pointed at
     * the copies next to the page. The content id is the file name without its suffix, so the suffix
     * has to be put back - without it every logo in the preview is a broken image.
     */
    private static String browserReadable(String html) {
        return CID_REFERENCE.matcher(html).replaceAll("src=\"$1.png\"");
    }

    private static void copyLogos() throws IOException {
        try (var logos = Files.list(IMAGE_SOURCE_DIR)) {
            logos.forEach(logo -> {
                try {
                    Files.copy(logo, PREVIEW_DIR.resolve(logo.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new UncheckedIOException("Could not copy the logo %s".formatted(logo), e);
                }
            });
        }
    }

    private static void write(Path path, String content) {
        try {
            Files.writeString(path, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write the preview %s".formatted(path), e);
        }
    }
}
