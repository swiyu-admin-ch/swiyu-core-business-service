package ch.admin.bj.swiyu.core.business.modules.payment.config;

import ch.admin.bj.swiyu.core.business.common.config.FunctionalityProperties;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * Checks the payment configuration at startup, but only when the payment feature is enabled. This way a
 * stage that does not offer payments yet can start without any payment values, while a stage with the
 * feature enabled still fails fast on a missing value.
 */
@Configuration
@RequiredArgsConstructor
public class PaymentConfig {

    public static final String TEMPLATE_PREFIX = "templates/pdf/";
    public static final String TEMPLATE_SUFFIX = ".html";

    private final FunctionalityProperties functionalityProperties;
    private final PaymentProperties paymentProperties;

    @PostConstruct
    public void validateWhenPaymentEnabled() {
        if (!Boolean.TRUE.equals(functionalityProperties.paymentEnabled())) {
            return;
        }
        var missing = new ArrayList<String>();
        if (paymentProperties.allowedReturnOrigins().isEmpty()) {
            missing.add("payment.allowed-return-origins");
        }
        if (!StringUtils.hasText(paymentProperties.saferpay().terminalId())) {
            missing.add("payment.saferpay.terminal-id");
        }
        if (!StringUtils.hasText(paymentProperties.saferpay().customerId())) {
            missing.add("payment.saferpay.customer-id");
        }
        var baseUrl = paymentProperties.saferpay().baseUrl();
        if (baseUrl == null || !StringUtils.hasText(baseUrl.toString())) {
            missing.add("payment.saferpay.base-url");
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                "The payment feature is enabled, but these properties are not set: " + String.join(", ", missing)
            );
        }
    }

    /**
     * MessageSource for receipt translations.
     * Loads properties from classpath:templates/pdf/receipt*.properties
     */
    @Bean
    public MessageSource receiptMessageSource() {
        return createReceiptMessageSource();
    }

    public static MessageSource createReceiptMessageSource() {
        var messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:templates/pdf/receipt");
        messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
        messageSource.setCacheSeconds(3600);
        messageSource.setFallbackToSystemLocale(false);
        return messageSource;
    }

    /**
     * Template engine for payment receipt PDF generation. Separate from the email template engine to allow
     * different template locations and configuration.
     *
     * <p>Uses {@link TemplateMode#HTML} since the templates are HTML documents that will be converted to PDF via
     * Flying-Saucer. HTML mode escapes every resolved variable, which is why the templates use
     * {@code [[${...}]]} and {@code th:text} and never the unescaped {@code [(${...})]} form.
     */
    @Bean
    public TemplateEngine receiptTemplateEngine() {
        return createReceiptTemplateEngine();
    }

    public static TemplateEngine createReceiptTemplateEngine() {
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

    @Bean("transactionTemplateRequiresNew")
    @ConditionalOnProperty(name = "app.functionality.payment-enabled", havingValue = "true", matchIfMissing = false)
    public TransactionTemplate transactionTemplateRequiresNew(PlatformTransactionManager transactionManager) {
        var template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template;
    }
}
