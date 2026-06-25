package ch.admin.bj.swiyu.core.business.modules.dataimport.domain;

import ch.admin.bj.swiyu.core.business.common.demodata.DemoDataConstants;
import ch.admin.bj.swiyu.core.business.common.domain.Address;
import ch.admin.bj.swiyu.core.business.common.domain.Contact;
import ch.admin.bj.swiyu.core.business.common.domain.MultiLanguageText;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.Signatory;
import java.util.List;
import java.util.UUID;
import lombok.experimental.UtilityClass;

@SuppressWarnings({ "java:S1192" })
@UtilityClass
public class CoreDemoData {

    public static final UUID CORE_ID_BP_DEFAULT = UUID.fromString("9f425029-9775-4984-99ba-bacc60069502");
    static final String CORE_ID_BP_WANTS_TO_BE_TRUSTED_S = "897edd6b-2e3e-4cc2-95a8-5b759c301df8";
    public static final UUID CORE_ID_BP_WANTS_TO_BE_TRUSTED = UUID.fromString(CORE_ID_BP_WANTS_TO_BE_TRUSTED_S);
    public static final UUID CORE_ID_BP_GOV = UUID.fromString("39f92e48-619e-4e92-8958-468ae138d8a3");
    public static final UUID CORE_ID_BP_BASE_ONBOARDING_ONLY = UUID.fromString("e97e84e6-f40e-47ba-bdfe-d92f3d3dbc84");

    public static final UUID CORE_ID_TOS_UNSUBMITTED = UUID.fromString(
        DemoDataConstants.TrustOnboardingSubmission.ID_UNSUBMITTED
    );
    public static final UUID CORE_ID_TOS_REJECTED = UUID.fromString("913a09b4-6f6b-4703-a682-1046ccb26abb");
    public static final UUID CORE_ID_TOS_SUBMITTED = UUID.fromString("3299cd25-8bab-47b7-9d46-f740be76e57e");
    public static final UUID CORE_ID_TOS_SUCCEEDED = UUID.fromString(
        DemoDataConstants.TrustOnboardingSubmission.ID_SUCCEEDED
    );
    public static final UUID CORE_ID_TOS_INFO_REQUESTED = UUID.fromString("dc828a98-ffb1-4ae4-8f07-b35d2818ac87");

    public static final String CORE_ID_BP_BASE_ONBOARDING_ONLY_PHONE = "+41791234567";
    public static final String CORE_ID_BP_DEFAULT_PHONE = "+41791234567";

    // CORE_ID_BP_DEFAULT
    public static final MultiLanguageText CORE_ID_BP_DEFAULT_NAMES = new MultiLanguageText(
        "Vertrau mir Beratung GmbH (DE)",
        "Confiance Conseil GmbH (FR)",
        "Trusty Consulting S.r.l. (IT)",
        "Trusty Consulting GmbH (EN)",
        "Trusty Consulting GmbH (RM)"
    );
    public static final Address CORE_ID_BP_DEFAULT_ADDRESS = new Address(
        "Geschäftsstraße 13",
        "Demohausen",
        "1111",
        "Schweiz",
        "Democanton"
    );
    public static final String CORE_ID_BP_DEFAULT_EMAIL = "erika.mueller@trusty-consulting.com";
    public static final Contact CORE_ID_BP_DEFAULT_CONTACT = new Contact(
        "erika",
        "müller",
        CORE_ID_BP_DEFAULT_EMAIL,
        "+41548884440",
        CORE_ID_BP_DEFAULT_ADDRESS
    );

    public static final List<Signatory> CORE_ID_BP_DEFAULT_SIGNATORIES = List.of(
        new Signatory("Erika", "Müller", "+41776665544", CORE_ID_BP_DEFAULT_EMAIL)
    );

    // CORE_ID_BP_WANTS_TO_BE_TRUSTED
    public static final MultiLanguageText CORE_ID_BP_WANTS_TO_BE_TRUSTED_NAMES = new MultiLanguageText(
        "Böswilliges Umzugsunternehmen GmbH",
        "Déménageurs malveillants GmbH",
        "Traslocatori malintenzionati S.r.l.",
        "Malicious Movers GmbH",
        "M. M. GmbH"
    );
    public static final Address CORE_ID_BP_WANTS_TO_BE_TRUSTED_ADDRESS = new Address(
        "Glitterallee 42",
        "Demohausen",
        "1111",
        "Steueroase",
        "Democanton"
    );
    public static final String CORE_ID_BP_WANTS_TO_BE_TRUSTED_EMAIL = "ceo@m-m.com";
    public static final Contact CORE_ID_BP_WANTS_TO_BE_TRUSTED_CONTACT = new Contact(
        "John",
        "Doe",
        CORE_ID_BP_WANTS_TO_BE_TRUSTED_EMAIL,
        "+41548884441",
        CORE_ID_BP_WANTS_TO_BE_TRUSTED_ADDRESS
    );

    // CORE_ID_BP_GOV
    public static final MultiLanguageText CORE_ID_BP_GOV_NAMES = new MultiLanguageText(
        "Demo Kanton",
        "Demo Canton",
        "Demo Cantone",
        "Demo Canton",
        "Demochaun"
    );
    public static final Address CORE_ID_BP_GOV_ADDRESS = new Address(
        "Erfolgsstrasse 1",
        "Demohausen",
        "11111",
        "Schweiz",
        "Democanton"
    );
    public static final String CORE_ID_BP_GOV_EMAIL = "s.schmid@democanton.admin.ch";
    public static final String CORE_ID_BP_GOV_EMAIL_JOHN = "j.doe@democanton.admin.ch";
    public static final String CORE_ID_BP_GOV_EMAIL_ERIKA = "e.mueller@democanton.admin.ch";
    public static final Contact CORE_ID_BP_GOV_CONTACT = new Contact(
        "Sandra",
        "Schmid",
        CORE_ID_BP_GOV_EMAIL,
        "+41216548497",
        CORE_ID_BP_GOV_ADDRESS
    );
    public static final List<Signatory> CORE_ID_BP_GOV_SIGNATORIES = List.of(
        new Signatory("Sandra", "Schmid", "+41665554433", CORE_ID_BP_GOV_EMAIL),
        new Signatory("John", "Doe", "+41776665544", CORE_ID_BP_GOV_EMAIL_JOHN),
        new Signatory("Erika", "Müller", "+41554443322", CORE_ID_BP_GOV_EMAIL_ERIKA)
    );

    // CORE_ID_BP_BASE_ONBOARDING_ONLY
    public static final MultiLanguageText CORE_ID_BP_BASE_ONBOARDING_ONLY_NAMES = new MultiLanguageText(
        "Demo Unternehmen",
        "Démonstration Entreprise",
        "Demo Azienda",
        "Demo Company",
        "Demo Unternehmen"
    );
    public static final Address CORE_ID_BP_BASE_ONBOARDING_ONLY_ADDRESS = new Address(
        "Geschäftsstraße 19",
        "Demohausen",
        "1111",
        "Schweiz",
        "Democanton"
    );
    public static final String CORE_ID_BP_BASE_ONBOARDING_ONLY_EMAIL = "helvetica@demo-comp.com";
}
