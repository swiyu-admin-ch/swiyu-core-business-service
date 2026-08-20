package ch.admin.bj.swiyu.core.business.modules.email.config;

import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

@Configuration
public class MailConfig {

    public static final String TEMPLATE_PREFIX = "email-templates/";
    public static final String TEMPLATE_SUFFIX = ".txt";

    /**
     * Template engine for the partner notification emails. These are plain text only, hence
     * {@link TemplateMode#TEXT} - no HTML escaping is applied to the resolved variables.
     */
    @Bean
    public TemplateEngine emailTemplateEngine() {
        var resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix(TEMPLATE_PREFIX);
        resolver.setSuffix(TEMPLATE_SUFFIX);
        resolver.setTemplateMode(TemplateMode.TEXT);
        resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resolver.setCacheable(true);

        var engine = new TemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }
}
