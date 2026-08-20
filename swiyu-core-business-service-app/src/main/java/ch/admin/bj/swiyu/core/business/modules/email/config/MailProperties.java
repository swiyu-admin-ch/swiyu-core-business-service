package ch.admin.bj.swiyu.core.business.modules.email.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.text.MessageFormat;
import java.util.UUID;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "app.mail")
public class MailProperties {

    /**
     * Sender address of all partner notification emails. Also used as the {@code contactEmail}
     * template variable.
     */
    @NotBlank
    private String from;

    /**
     * Reply-to address of all partner notification emails.
     */
    @NotBlank
    private String replyTo;

    /**
     * Stage marker prepended to every subject, e.g. "[DEV] ". Empty on PROD.
     */
    @NotNull
    private String subjectPrefix = "";

    /**
     * Service portal links embedded in the notification emails.
     */
    @Valid
    @NotNull
    private ServicePortal servicePortal = new ServicePortal();

    @Data
    public static class ServicePortal {

        /**
         * Base URL of the partner service portal. Not read directly - it exists so a stage only has
         * to configure one URL, from which {@link #partnerTemplateUrl} defaults are composed.
         */
        @NotBlank
        private String baseUrl;

        /**
         * {@link MessageFormat} template of the partner detail page, with the partner id as
         * {@code {0}}, following the same convention as the identifier registry route templates.
         */
        @NotBlank
        private String partnerTemplateUrl;

        public String partnerUrl(UUID partnerId) {
            return MessageFormat.format(partnerTemplateUrl, partnerId);
        }
    }
}
