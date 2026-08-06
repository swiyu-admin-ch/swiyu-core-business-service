package ch.admin.bj.swiyu.core.business.modules.trust.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.RequestParam;

@Schema(name = "ProtectedVerificationSubmissionFilter")
public record ProtectedVerificationSubmissionFilterDto(@RequestParam(required = false) List<UUID> businessPartnerIds) {
    public ProtectedVerificationSubmissionFilterDto of(List<UUID> businessPartnerIds) {
        return new ProtectedVerificationSubmissionFilterDto(businessPartnerIds);
    }
}
