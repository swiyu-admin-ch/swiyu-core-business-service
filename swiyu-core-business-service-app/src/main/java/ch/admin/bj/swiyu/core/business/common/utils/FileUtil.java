package ch.admin.bj.swiyu.core.business.common.utils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.experimental.UtilityClass;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

@UtilityClass
public class FileUtil {

    private static final String DEFAULT_FILE_BASENAME_PREFIX = "file_";
    // https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-keys.html#object-key-guidelines-safe-characters
    private static final Pattern DISALLOWED_CHARS = Pattern.compile("[^a-zA-Z0-9!*._()-]");

    // https://www.unicode.org/reports/tr44/#General_Category_Values
    // https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/regex/Pattern.html#ubpc
    private static final Pattern DIACRITIC_MARKS_AND_CONTROL_CHARS_PATTERN = Pattern.compile("[\\p{M}\\p{Cntrl}]");

    public static String asString(Resource resource) {
        try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Sanitizes a filename to be safe for storage and transmission.
     * Removes path traversal, diacritics, control characters, and any characters not allowed by S3 object‑key guidelines.
     * Returns a fallback name when the input is null, empty, or results in an invalid filename.
     */
    @SuppressWarnings("java:S2259") // `baseName` cannot be null, see documentation of `getFilename`
    public static String sanitizeFilename(String unsanitizedInput) {
        if (unsanitizedInput == null) {
            return fallbackFilename();
        }

        // Remove a potential folder or path traversal prefix
        var normalizedSlashes = unsanitizedInput.replace('\\', '/');
        var baseName = StringUtils.getFilename(normalizedSlashes);
        if (baseName.isEmpty() || ".".equals(baseName) || "..".equals(baseName)) {
            return fallbackFilename();
        }

        // Normalize ä -> a + ¨
        var normalized = Normalizer.normalize(baseName, Normalizer.Form.NFD);
        // Remove diacritic marks (¨, ´, etc) and control characters.
        var withoutMarksAndControlChars = DIACRITIC_MARKS_AND_CONTROL_CHARS_PATTERN.matcher(normalized).replaceAll("");

        var sanitized = DISALLOWED_CHARS.matcher(withoutMarksAndControlChars).replaceAll("_");
        if (sanitized.isEmpty() || sanitized.chars().allMatch(c -> c == '_')) {
            return fallbackFilename();
        }

        return sanitized;
    }

    private static String fallbackFilename() {
        return DEFAULT_FILE_BASENAME_PREFIX + UUID.randomUUID();
    }
}
