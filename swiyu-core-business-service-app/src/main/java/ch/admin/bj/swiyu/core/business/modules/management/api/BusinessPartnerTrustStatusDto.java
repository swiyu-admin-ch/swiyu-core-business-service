package ch.admin.bj.swiyu.core.business.modules.management.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    name = "BusinessPartnerTrustStatus",
    description = """
    Defines the current aggregated state of the trust process.
        @startuml
        [*] --> NOT_VERIFIED

        NOT_VERIFIED -[#green]-> VERIFICATION_STARTED

        VERIFICATION_STARTED -[#green]-> VERIFICATION_IN_PROGRESS

        VERIFICATION_IN_PROGRESS -[#orange]-> INFORMATION_REQUESTED
        VERIFICATION_IN_PROGRESS -[#green]-> VERIFIED
        VERIFICATION_IN_PROGRESS -[#red]-> NOT_VERIFIED

        INFORMATION_REQUESTED -[#green]-> VERIFIED
        INFORMATION_REQUESTED -[#red]-> NOT_VERIFIED

        VERIFIED -[#green]-> RE_VERIFICATION_STARTED

        RE_VERIFICATION_STARTED -[#green]-> RE_VERIFICATION_IN_PROGRESS

        RE_VERIFICATION_IN_PROGRESS -[#orange]-> INFORMATION_REQUESTED
        RE_VERIFICATION_IN_PROGRESS -[#green]-> VERIFIED
        RE_VERIFICATION_IN_PROGRESS -[#red]-> NOT_VERIFIED
        @enduml
    """,
    enumAsRef = true
)
public enum BusinessPartnerTrustStatusDto {
    NOT_VERIFIED,
    VERIFICATION_STARTED,
    VERIFICATION_IN_PROGRESS,
    INFORMATION_REQUESTED,
    VERIFIED,
    RE_VERIFICATION_STARTED,
    RE_VERIFICATION_IN_PROGRESS,
}
