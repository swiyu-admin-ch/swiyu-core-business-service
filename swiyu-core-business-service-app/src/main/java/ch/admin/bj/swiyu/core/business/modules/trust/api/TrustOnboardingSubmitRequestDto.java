package ch.admin.bj.swiyu.core.business.modules.trust.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

@Builder
@Schema(name = "TrustOnboardingSubmitRequest")
public record TrustOnboardingSubmitRequestDto(@Getter @NotNull Long version) {}
