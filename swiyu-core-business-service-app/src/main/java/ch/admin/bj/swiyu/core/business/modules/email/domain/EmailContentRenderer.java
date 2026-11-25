package ch.admin.bj.swiyu.core.business.modules.email.domain;

import ch.admin.bj.swiyu.core.business.modules.email.config.MailConfig;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Renders an {@link EmailType} template into a complete {@link RenderedEmail}.
 *
 * <p>A template is an HTML document with one {@code <section lang="...">} per language, in the order
 * DE / FR / IT / EN, and the subject - all four languages separated by "/" - in its {@code <title>}.
 * The stylesheet and the closing signature come from the fragments under
 * {@code templates/email/fragments}.
 *
 * <p>The subject is read back out of the rendered document rather than from a separate front matter
 * block, so it goes through Thymeleaf like everything else and may contain variables. The stage
 * prefix (e.g. "[DEV]") is prepended to it.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailContentRenderer {

    /**
     * The signature fragment is pulled into every template, so the variables it declares are
     * variables every template needs.
     */
    private static final String SIGNATURE_FRAGMENT = "fragments/signature";

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

    /** The document title, which is where the subject comes from. */
    private static final Pattern TITLE = Pattern.compile("(?is)<title\\b[^>]*>(.*?)</title\\s*>");

    private final TemplateEngine emailTemplateEngine;

    public RenderedEmail render(EmailType emailType, Map<String, Object> variables, String subjectPrefix) {
        logMissingVariables(emailType, variables);

        var context = new Context();
        variables.forEach(context::setVariable);
        var body = emailTemplateEngine.process(emailType.getTemplateName(), context);

        var subject = withPrefix(subjectPrefix, parseSubject(emailType, body));

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
        var missing = declaredVariables(emailType)
            .stream()
            .filter(name -> variables.get(name) == null)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        if (!missing.isEmpty()) {
            log.error(
                "Email template '{}' references variables that were not supplied or are null: {}. " +
                    "The rendered email will contain empty placeholders.",
                emailType.getTemplateName(),
                missing
            );
        }
    }

    private static void logUnresolvedExpressions(EmailType emailType, String rendered) {
        var unresolved = UNRESOLVED_EXPRESSION.matcher(rendered).results().map(MatchResult::group).toList();
        if (!unresolved.isEmpty()) {
            log.error(
                "Email template '{}' produced unresolved template expressions: {}",
                emailType.getTemplateName(),
                unresolved
            );
        }
    }

    /**
     * The subject is the document title.
     *
     * <p>The title carries accented text in four languages, so the character references Thymeleaf
     * emits have to be resolved the way the receiving mail client would - hence the decoding step
     * rather than taking the raw match.
     */
    private static String parseSubject(EmailType emailType, String rendered) {
        var match = TITLE.matcher(rendered);
        var title = match.find() ? HtmlToPlainTextConverter.decodeEntities(match.group(1)).strip() : "";
        if (title.isEmpty()) {
            throw new IllegalStateException(
                "Email template '%s' has no <title> to take the subject from".formatted(emailType.getTemplateName())
            );
        }
        return title;
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

    private String readSource(String templateName) {
        var path = MailConfig.TEMPLATE_PREFIX + templateName + MailConfig.TEMPLATE_SUFFIX;
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
     * Exposed so callers can assert on the exact set of variables a template needs. Includes the
     * signature fragment, which every template pulls in and which declares {@code contactEmail}.
     */
    public Set<String> declaredVariables(EmailType emailType) {
        var source = readSource(emailType.getTemplateName()) + readSource(SIGNATURE_FRAGMENT);
        return TEMPLATE_VARIABLE.matcher(source)
            .results()
            .map(match -> match.group(1))
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
