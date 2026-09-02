package ch.admin.bj.swiyu.core.business.common.audit;

import ch.admin.bit.jeap.audit.record.create.AuditEventType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;

/**
 * See <a href="https://confluence.bit.admin.ch/x/gPVCTg">https://confluence.bit.admin.ch/x/gPVCTg</a> for all defined
 * UseCases.
 */
@Getter
@RequiredArgsConstructor
public enum AuditUseCase {
    STATUS_LIST_CHANGED(
        Category.STATUS_REGISTRY,
        ObjectType.STATUS_LIST,
        DataJsonFieldName.STATUS_LIST_META,
        DataValueFieldName.STATUS_LIST_JWT,
        AuditEventType.MODIFIED
    ),
    STATUS_LIST_CREATED(
        Category.STATUS_REGISTRY,
        ObjectType.STATUS_LIST,
        DataJsonFieldName.STATUS_LIST_META,
        null,
        AuditEventType.CREATED
    ),
    IDENTIFIER_ENTRY_CREATED(
        Category.IDENTIFIER_REGISTRY,
        ObjectType.IDENTIFIER_ENTRY,
        DataJsonFieldName.IDENTIFIER_ENTRY_META,
        null, // no non-json value sent
        AuditEventType.CREATED
    ),
    IDENTIFIER_ENTRY_CHANGED(
        Category.IDENTIFIER_REGISTRY,
        ObjectType.IDENTIFIER_ENTRY,
        DataJsonFieldName.IDENTIFIER_ENTRY_META,
        DataValueFieldName.IDENTIFIER_ENTRY_DID_DOC,
        AuditEventType.MODIFIED
    ),
    IDENTIFIER_ENTRY_DESCRIPTION_CHANGED(
        Category.IDENTIFIER_REGISTRY,
        ObjectType.IDENTIFIER_ENTRY,
        DataJsonFieldName.IDENTIFIER_ENTRY_META,
        null, // no non-json value sent
        AuditEventType.MODIFIED
    ),
    BUSINESS_PARTNER_REGISTERED(
        Category.BUSINESS_PARTNER,
        ObjectType.BUSINESS_PARTNER,
        DataJsonFieldName.BUSINESS_PARTNER_DATA,
        null, // no non-json value sent
        AuditEventType.CREATED
    ),
    BUSINESS_PARTNER_UPDATED(
        Category.BUSINESS_PARTNER,
        ObjectType.BUSINESS_PARTNER,
        DataJsonFieldName.BUSINESS_PARTNER_DATA,
        null,
        AuditEventType.MODIFIED
    ),
    TRUST_ONBOARDING_DOCUMENT_UPLOADED(
        Category.BUSINESS_PARTNER,
        ObjectType.BUSINESS_PARTNER_DOCUMENT,
        DataJsonFieldName.TRUST_ONBOARDING_DOCUMENT_META,
        null, // no non-json value sent (only s3 file)
        AuditEventType.MODIFIED
    ),
    TRUST_ONBOARDING_SUBMITTED(
        Category.BUSINESS_PARTNER,
        ObjectType.TRUST_ONBOARDING_SUBMISSION,
        DataJsonFieldName.TRUST_ONBOARDING_SUBMISSION_DATA,
        null, // no non-json value sent
        AuditEventType.MODIFIED
    ),
    EMAIL_SENT(
        Category.NOTIFICATION,
        ObjectType.EMAIL,
        DataJsonFieldName.EMAIL_DATA,
        null, // no non-json value sent
        AuditEventType.CREATED
    );

    private final String category;
    private final String auditObjectType;
    /** Key under which the entity's JSON snapshot is stored in the audit record's object data. */
    private final String dataJsonFieldName;
    /** Key under which a non-json document is stored (e.g. a JWT or DID log) is stored. {@code null} if this use case has no secondary document. */
    private final String dataValueFieldName;
    private final AuditEventType eventType;

    @UtilityClass
    private static final class Category {

        static final String STATUS_REGISTRY = "STATUS_REGISTRY";
        static final String IDENTIFIER_REGISTRY = "IDENTIFIER_REGISTRY";
        static final String BUSINESS_PARTNER = "BUSINESS_PARTNER";
        static final String NOTIFICATION = "NOTIFICATION";
    }

    @UtilityClass
    private static final class ObjectType {

        static final String EMAIL = "EMAIL";
        static final String STATUS_LIST = "STATUS_LIST";
        static final String IDENTIFIER_ENTRY = "IDENTIFIER_ENTRY";
        static final String BUSINESS_PARTNER = "BUSINESS_PARTNER";
        static final String BUSINESS_PARTNER_DOCUMENT = "BUSINESS_PARTNER_DOCUMENT";
        static final String TRUST_ONBOARDING_SUBMISSION = "TRUST_ONBOARDING_SUBMISSION";
    }

    @UtilityClass
    static final class DataJsonFieldName {

        static final String STATUS_LIST_META = "STATUS_LIST_META";
        static final String IDENTIFIER_ENTRY_META = "IDENTIFIER_ENTRY_META";
        static final String BUSINESS_PARTNER_DATA = "BUSINESS_PARTNER_DATA";
        static final String TRUST_ONBOARDING_DOCUMENT_META = "TRUST_ONBOARDING_DOCUMENT_META";
        static final String TRUST_ONBOARDING_SUBMISSION_DATA = "TRUST_ONBOARDING_SUBMISSION_DATA";
        static final String EMAIL_DATA = "EMAIL_DATA";
    }

    @UtilityClass
    private static final class DataValueFieldName {

        static final String IDENTIFIER_ENTRY_DID_DOC = "IDENTIFIER_ENTRY_DID_DOC";
        static final String STATUS_LIST_JWT = "STATUS_LIST_JWT";
    }

    @UtilityClass
    static final class DataS3FieldName {

        public static final String TRUST_ONBOARDING_DOCUMENT = "TRUST_ONBOARDING_DOCUMENT";
    }
}
