package ch.admin.bj.swiyu.core.business.modules.email.domain;

import ch.admin.bj.swiyu.core.business.modules.email.config.MailConfig;
import java.util.HashMap;
import java.util.Map;

/**
 * The one place that says how a template is rendered in a test.
 *
 * <p>Five tests around the HTML templates each need a renderer and the variables the templates
 * declare, and each of them had its own copy. The copies are the problem: the set of variables is not
 * decoration, it is the contract between the templates and
 * {@link EmailContentRenderer#declaredVariables}, and a template that starts using a new variable has
 * to be a change in one place rather than four - four being the number where one gets missed.
 */
final class EmailTemplateFixture {

    static final String CONTACT_EMAIL = "registries@swiyu.admin.ch";

    /**
     * A real looking portal URL rather than a placeholder: {@code DevHtmlEmailPreviewTest} renders the
     * pages the translation reviewers and the sprint review are shown, and "a-partner-id" in the
     * middle of a link is the kind of detail that gets asked about instead of the email.
     */
    static final String PARTNER_URL =
        "https://portal.trust-infra.swiyu.admin.ch/ui/business-partners/6bd0dc1c-6f36-4bd8-b2ad-38cbb8cf4c2e";

    /**
     * Only the repeated renewal reminder counts days down. The initial reminder states its 180 day
     * period in the reviewed text and the delayed review notice names no period at all, so neither
     * declares the variable.
     */
    static final int EXPIRATION_DAYS = 90;

    private EmailTemplateFixture() {}

    static EmailContentRenderer renderer() {
        return new EmailContentRenderer(new MailConfig().emailTemplateEngine());
    }

    /**
     * Every variable a template of that type declares, and no other. Deliberately exact: a template
     * referencing a variable nobody supplies renders a silent gap, and
     * {@code EmailContentRendererTest} asserts against this set to catch it.
     */
    static Map<String, Object> variablesFor(EmailType emailType) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("contactEmail", CONTACT_EMAIL);
        if (emailType == EmailType.BASE_REGISTRY_DID_INITIALIZED) {
            // This email links to the partner page with the DID notification panel preselected
            variables.put("servicePortalPartnerDidNotificationUrl", PARTNER_URL + "?notification=did-added");
        } else {
            variables.put("servicePortalPartnerUrl", PARTNER_URL);
        }
        if (emailType == EmailType.TRUST_RENEWAL_REMINDER) {
            variables.put("expirationDurationDays", EXPIRATION_DAYS);
        }
        return variables;
    }
}
