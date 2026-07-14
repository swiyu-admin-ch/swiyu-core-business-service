package ch.admin.bj.swiyu.core.business.common.domain;

public enum BusinessPartnerType {
    GOVERNMENTAL_INSTITUTION,
    BUSINESS,
    INDIVIDUAL,
    /**
     * @deprecated since 3.40.3. Legacy placeholder for partners onboarded before the type concept was introduced.
     * All existing UNKNOWN entries are migrated to BUSINESS. The new v2 onboarding flow must never produce this type.
     */
    @SuppressWarnings("java:S1133")
    @Deprecated(since = "3.40.3")
    UNKNOWN,
}
