package ch.admin.bj.swiyu.core.business.common.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "IdentifierStatus")
public enum IdentifierStatusDto {
    NOT_INITIALIZED,
    INITIALIZED,
    USER_DEACTIVATED,
    // we currently only support did:tdw:0.3 and up
    DEACTIVATED_BY_MIGRATION_BECAUSE_OF_UNSUPPORTED_FORMAT,
}
