package ch.admin.bj.swiyu.core.business.common.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FileUtilTest {

    @Test
    void sanitizeFilename_removesPathTraversalAndUnsafeChars() {
        var sanitized = FileUtil.sanitizeFilename("../../folder/ä-invoice\u0000 (final).pdf");
        assertThat(sanitized).isEqualTo("a-invoice_(final).pdf");
    }

    @Test
    void sanitizeFilename_normalizesUnicodeForS3Key() {
        var sanitized = FileUtil.sanitizeFilename("äöü-éè-Report.pdf");
        assertThat(sanitized).isEqualTo("aou-ee-Report.pdf");
    }

    @Test
    void sanitizeFilename_handlesNullAndEmptyInputWithFallback() {
        assertThat(FileUtil.sanitizeFilename(null)).startsWith("file_");
        assertThat(FileUtil.sanitizeFilename("")).startsWith("file_");
    }

    @Test
    void sanitizeFilename_handlesRootPath() {
        assertThat(FileUtil.sanitizeFilename("/")).startsWith("file_");
    }

    @Test
    void sanitizeFilename_handlesOnlyDots() {
        assertThat(FileUtil.sanitizeFilename("..")).startsWith("file_");
        assertThat(FileUtil.sanitizeFilename(".")).startsWith("file_");
    }

    @Test
    void sanitizeFilename_handlesInvalidPathInputWithoutThrowing() {
        var sanitized = FileUtil.sanitizeFilename("doc\u0000name.pdf");
        assertThat(sanitized).isEqualTo("docname.pdf");
    }

    @Test
    void sanitizeFilename_preventsCRLFInjection() {
        var sanitized = FileUtil.sanitizeFilename("normal.pdf\r\nSet-Cookie: session=evil");
        assertThat(sanitized).doesNotContain("\r").doesNotContain("\n");
    }

    @Test
    void sanitizeFilename_preventsQuoteBreakout() {
        var sanitized = FileUtil.sanitizeFilename("file.txt\"; extension=\".exe");
        assertThat(sanitized).isEqualTo("file.txt___extension__.exe");
    }

    @Test
    void sanitizeFilename_preventsSemicolonParameterInjection() {
        var sanitized = FileUtil.sanitizeFilename("standard.txt; format=hack");
        assertThat(sanitized).isEqualTo("standard.txt__format_hack");
    }

    @Test
    void sanitizeFilename_preventsNullByteTruncation() {
        var sanitized = FileUtil.sanitizeFilename("malicious.exe\0.pdf");
        assertThat(sanitized).isEqualTo("malicious.exe.pdf");
    }

    @Test
    void sanitizeFilename_preventsPathTraversal_Linux() {
        var sanitized = FileUtil.sanitizeFilename("../../../etc/passwd");
        assertThat(sanitized).isEqualTo("passwd");
    }

    @Test
    void sanitizeFilename_preventsPathTraversal_Windows() {
        var sanitized = FileUtil.sanitizeFilename("C:\\Windows\\System32\\cmd.exe");
        assertThat(sanitized).isEqualTo("cmd.exe");
    }
}
