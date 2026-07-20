package ch.admin.bj.swiyu.core.business.modules.identifier.api;

import ch.admin.bj.swiyu.core.business.common.api.CountLimitDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(name = "IdentifierEntryLimits")
@NotNull
public record IdentifierEntryLimitsDto(@NotNull CountLimitDto count) {}
