package ch.admin.bj.swiyu.core.business.modules.trust.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.RequestParam;

@Schema(name = "TrustOnboardingSubmissionFilter")
public record TrustOnboardingSubmissionFilterDto(@RequestParam(required = false) List<UUID> businessPartnerIds) {
    public TrustOnboardingSubmissionFilterDto of(List<UUID> businessPartnerIds) {
        return new TrustOnboardingSubmissionFilterDto(businessPartnerIds);
    }
}
