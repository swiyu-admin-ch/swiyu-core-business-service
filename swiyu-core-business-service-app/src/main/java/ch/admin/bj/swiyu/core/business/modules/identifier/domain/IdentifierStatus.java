package ch.admin.bj.swiyu.core.business.modules.identifier.domain;

public enum IdentifierStatus {
    NOT_INITIALIZED,
    INITIALIZED,
    USER_DEACTIVATED,
    // we currently only support did:tdw:0.3 and up
    DEACTIVATED_BY_MIGRATION_BECAUSE_OF_UNSUPPORTED_FORMAT,
}
