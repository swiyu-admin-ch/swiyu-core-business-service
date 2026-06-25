package ch.admin.bj.swiyu.core.business.common.audit;

import ch.admin.bit.jeap.audit.record.create.AuditEventType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;

@Getter
@RequiredArgsConstructor
public enum AuditUseCase {
    STATUS_LIST_CHANGED(
        "STATUS_LIST_CHANGED",
        Category.STATUS_REGISTRY,
        ObjectType.STATUS_LIST,
        MetaFieldName.STATUS_LIST_META,
        "STATUS_LIST_JWT",
        AuditEventType.MODIFIED
    ),
    STATUS_LIST_CREATED(
        "STATUS_LIST_CREATED",
        Category.STATUS_REGISTRY,
        ObjectType.STATUS_LIST,
        MetaFieldName.STATUS_LIST_META,
        null,
        AuditEventType.CREATED
    ),
    IDENTIFIER_ENTRY_CREATED(
        "IDENTIFIER_ENTRY_CREATED",
        Category.IDENTIFIER_REGISTRY,
        ObjectType.IDENTIFIER_ENTRY,
        MetaFieldName.IDENTIFIER_ENTRY_META,
        null,
        AuditEventType.CREATED
    ),
    IDENTIFIER_ENTRY_CHANGED(
        "IDENTIFIER_ENTRY_CHANGED",
        Category.IDENTIFIER_REGISTRY,
        ObjectType.IDENTIFIER_ENTRY,
        MetaFieldName.IDENTIFIER_ENTRY_META,
        "IDENTIFIER_ENTRY_DID_DOC",
        AuditEventType.MODIFIED
    ),
    IDENTIFIER_ENTRY_DESCRIPTION_CHANGED(
        "IDENTIFIER_ENTRY_DESCRIPTION_CHANGED",
        Category.IDENTIFIER_REGISTRY,
        ObjectType.IDENTIFIER_ENTRY,
        MetaFieldName.IDENTIFIER_ENTRY_META,
        null,
        AuditEventType.MODIFIED
    ),
    BUSINESS_PARTNER_REGISTERED(
        "BUSINESS_PARTNER_REGISTERED",
        Category.MANAGEMENT,
        ObjectType.BUSINESS_PARTNER,
        MetaFieldName.BUSINESS_PARTNER_DATA,
        null,
        AuditEventType.CREATED
    ),
    BUSINESS_PARTNER_UPDATED(
        "BUSINESS_PARTNER_UPDATED",
        Category.MANAGEMENT,
        ObjectType.BUSINESS_PARTNER,
        MetaFieldName.BUSINESS_PARTNER_DATA,
        null,
        AuditEventType.MODIFIED
    ),
    TRUST_ONBOARDING_DOCUMENT_UPLOADED(
        "TRUST_ONBOARDING_DOCUMENT_UPLOADED",
        Category.TRUST,
        ObjectType.TRUST_ONBOARDING_DOCUMENT,
        null,
        null,
        AuditEventType.MODIFIED
    ),
    TRUST_ONBOARDING_SUBMITTED(
        "TRUST_ONBOARDING_SUBMITTED",
        Category.TRUST,
        ObjectType.TRUST_ONBOARDING_SUBMISSION,
        MetaFieldName.TRUST_ONBOARDING_SUBMISSION_DATA,
        null,
        AuditEventType.MODIFIED
    );

    private final String name;
    private final String category;
    private final String auditObjectType;
    /** Key under which the entity's JSON snapshot is stored in the audit record's object data. */
    private final String metaFieldName;
    /** Key under which a secondary text document (e.g. a JWT or DID log) is stored. {@code null} if this use case has no secondary document. */
    private final String documentFieldName;
    private final AuditEventType eventType;

    @UtilityClass
    private static final class Category {

        static final String STATUS_REGISTRY = "STATUS_REGISTRY";
        static final String IDENTIFIER_REGISTRY = "IDENTIFIER_REGISTRY";
        static final String MANAGEMENT = "MANAGEMENT";
        static final String TRUST = "TRUST";
    }

    @UtilityClass
    private static final class ObjectType {

        static final String STATUS_LIST = "STATUS_LIST";
        static final String IDENTIFIER_ENTRY = "IDENTIFIER_ENTRY";
        static final String BUSINESS_PARTNER = "BUSINESS_PARTNER";
        static final String TRUST_ONBOARDING_DOCUMENT = "TRUST_ONBOARDING_DOCUMENT";
        static final String TRUST_ONBOARDING_SUBMISSION = "TRUST_ONBOARDING_SUBMISSION";
    }

    @UtilityClass
    private static final class MetaFieldName {

        static final String STATUS_LIST_META = "STATUS_LIST_META";
        static final String IDENTIFIER_ENTRY_META = "IDENTIFIER_ENTRY_META";
        static final String BUSINESS_PARTNER_DATA = "BUSINESS_PARTNER_DATA";
        static final String TRUST_ONBOARDING_SUBMISSION_DATA = "TRUST_ONBOARDING_SUBMISSION_DATA";
    }
}
