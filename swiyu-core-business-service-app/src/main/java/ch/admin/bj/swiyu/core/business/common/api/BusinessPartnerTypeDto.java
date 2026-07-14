package ch.admin.bj.swiyu.core.business.common.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    name = "BusinessPartnerType",
    enumAsRef = true,
    description = """
    | Value                        | Description                                                   |
    | ---------------------------- | ------------------------------------------------------------- |
    | GOVERNMENTAL_INSTITUTION     | Governmental institutions like JPD or ASTRA                   |
    | BUSINESS                     | Business entities and corporations                            |
    | INDIVIDUAL                   | Individual persons                                            |
    | UNKNOWN                      | Deprecated – legacy placeholder, all entries migrated to BUSINESS |
    """
)
public enum BusinessPartnerTypeDto {
    GOVERNMENTAL_INSTITUTION,
    BUSINESS,
    INDIVIDUAL,
    /**
     * @deprecated since 3.40.3. Legacy placeholder – all existing entries have been migrated to BUSINESS.
     * Must not be used in new v2 onboarding requests.
     */
    @SuppressWarnings("java:S1133")
    @Deprecated(since = "3.40.3")
    UNKNOWN,
}
