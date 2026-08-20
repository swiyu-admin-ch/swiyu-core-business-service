package ch.admin.bj.swiyu.core.business.modules.email.domain;

import ch.admin.bj.swiyu.core.business.modules.email.config.MailConfig;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Renders an {@link EmailType} template into a complete {@link RenderedEmail}.
 *
 * <p>A template consists of a front-matter block delimited by lines containing only {@code ---},
 * holding the subject in all four languages separated by "/", followed by the body with the
 * DE / FR / IT / EN sections:
 *
 * <pre>
 * ---
 * subject: Antrag eingereicht/ Application submitted/ Demande deposee/ Richiesta presentata
 * ---
 * Guten Tag
 * ...
 * </pre>
 *
 * <p>The whole file - front matter included - is passed through Thymeleaf, so the subject may
 * contain variables too. The stage prefix (e.g. "[DEV]") is prepended to the rendered subject.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailContentRenderer {

    private static final String FRONT_MATTER_DELIMITER = "---";
    private static final String SUBJECT_KEY = "subject";

    /**
     * Matches a variable reference in the raw template source, used to verify up front that the
     * caller supplied every variable the template needs.
     */
    private static final Pattern TEMPLATE_VARIABLE = Pattern.compile("\\$\\{\\s*([A-Za-z_][A-Za-z0-9_]*)\\s*[}.]");

    /**
     * Matches anything that still looks like a template expression after rendering. Thymeleaf
     * consumes every well formed expression, so a match means the template contains a malformed or
     * unprocessed placeholder.
     */
    private static final Pattern UNRESOLVED_EXPRESSION = Pattern.compile(
        "\\$\\{[^}]*}|\\[\\[[^\\]]*]]|\\[\\([^)]*\\)]"
    );

    private final TemplateEngine emailTemplateEngine;

    public RenderedEmail render(EmailType emailType, Map<String, Object> variables, String subjectPrefix) {
        logMissingVariables(emailType, variables);

        var context = new Context();
        variables.forEach(context::setVariable);
        var rendered = emailTemplateEngine.process(emailType.getTemplateName(), context);

        var subject = withPrefix(subjectPrefix, parseSubject(emailType, rendered));
        var body = stripFrontMatter(rendered).strip();

        logUnresolvedExpressions(emailType, subject);
        logUnresolvedExpressions(emailType, body);

        return new RenderedEmail(subject, body);
    }

    /**
     * Reads the template source straight from the classpath so we can compare the variables it
     * declares against the ones supplied. Thymeleaf resolves an unknown variable to null and renders
     * it as an empty string, which would otherwise leave a silent gap in the email.
     */
    private void logMissingVariables(EmailType emailType, Map<String, Object> variables) {
        var missing = new LinkedHashSet<String>();
        TEMPLATE_VARIABLE.matcher(readTemplateSource(emailType))
            .results()
            .map(match -> match.group(1))
            .filter(name -> variables.get(name) == null)
            .forEach(missing::add);
        if (!missing.isEmpty()) {
            log.error(
                "Email template '{}' references variables that were not supplied or are null: {}. " +
                    "The rendered email will contain empty placeholders.",
                emailType.getTemplateName(),
                missing
            );
        }
    }

    private void logUnresolvedExpressions(EmailType emailType, String rendered) {
        var unresolved = UNRESOLVED_EXPRESSION.matcher(rendered)
            .results()
            .map(java.util.regex.MatchResult::group)
            .toList();
        if (!unresolved.isEmpty()) {
            log.error(
                "Email template '{}' produced unresolved template expressions: {}",
                emailType.getTemplateName(),
                unresolved
            );
        }
    }

    private String parseSubject(EmailType emailType, String rendered) {
        for (var line : frontMatterLines(rendered)) {
            var separator = line.indexOf(':');
            if (separator > 0 && SUBJECT_KEY.equals(line.substring(0, separator).strip())) {
                var subject = line.substring(separator + 1).strip();
                if (!subject.isEmpty()) {
                    return subject;
                }
            }
        }
        throw new IllegalStateException(
            "Email template '%s' has no '%s' entry in its front matter".formatted(
                emailType.getTemplateName(),
                SUBJECT_KEY
            )
        );
    }

    /**
     * Prepends the stage marker, tolerating a prefix configured without a trailing space.
     */
    private static String withPrefix(String subjectPrefix, String subject) {
        if (subjectPrefix == null || subjectPrefix.isBlank()) {
            return subject;
        }
        return subjectPrefix.endsWith(" ") ? subjectPrefix + subject : subjectPrefix + " " + subject;
    }

    private List<String> frontMatterLines(String rendered) {
        var lines = rendered.strip().lines().toList();
        if (lines.isEmpty() || !FRONT_MATTER_DELIMITER.equals(lines.getFirst().strip())) {
            return List.of();
        }
        var end = indexOfClosingDelimiter(lines);
        return end < 0 ? List.of() : lines.subList(1, end);
    }

    private String stripFrontMatter(String rendered) {
        var lines = rendered.strip().lines().toList();
        if (lines.isEmpty() || !FRONT_MATTER_DELIMITER.equals(lines.getFirst().strip())) {
            return rendered;
        }
        var end = indexOfClosingDelimiter(lines);
        return end < 0 ? rendered : String.join("\n", lines.subList(end + 1, lines.size()));
    }

    private int indexOfClosingDelimiter(List<String> lines) {
        for (var i = 1; i < lines.size(); i++) {
            if (FRONT_MATTER_DELIMITER.equals(lines.get(i).strip())) {
                return i;
            }
        }
        return -1;
    }

    private String readTemplateSource(EmailType emailType) {
        var path = MailConfig.TEMPLATE_PREFIX + emailType.getTemplateName() + MailConfig.TEMPLATE_SUFFIX;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Email template '%s' not found on the classpath".formatted(path));
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read email template '%s'".formatted(path), e);
        }
    }

    /**
     * Exposed so callers can assert on the exact set of variables a template needs.
     */
    public Set<String> declaredVariables(EmailType emailType) {
        return TEMPLATE_VARIABLE.matcher(readTemplateSource(emailType))
            .results()
            .map(match -> match.group(1))
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }
}
