package ch.admin.bj.swiyu.core.business.modules.status.api;

import ch.admin.bj.swiyu.core.business.common.api.CountLimitDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(name = "StatusListEntryLimits")
@NotNull
public record StatusListEntryLimitsDto(@NotNull CountLimitDto count) {}
