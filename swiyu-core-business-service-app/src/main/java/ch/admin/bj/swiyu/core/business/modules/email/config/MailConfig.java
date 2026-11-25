package ch.admin.bj.swiyu.core.business.modules.email.config;

import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

@Configuration
public class MailConfig {

    public static final String TEMPLATE_PREFIX = "templates/email/";
    public static final String TEMPLATE_SUFFIX = ".html";

    /**
     * Template engine for the partner notification emails.
     *
     * <p>{@link TemplateMode#HTML} since EID-6921: the templates are HTML documents, and the plain
     * text alternative part of the multipart message is derived from the rendered HTML rather than
     * authored separately. HTML mode escapes every resolved variable, which is why the templates use
     * {@code [[${...}]]} and {@code th:text} and never the unescaped {@code [(${...})]} form.
     */
    @Bean
    public TemplateEngine emailTemplateEngine() {
        var resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix(TEMPLATE_PREFIX);
        resolver.setSuffix(TEMPLATE_SUFFIX);
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resolver.setCacheable(true);

        var engine = new TemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }
}
