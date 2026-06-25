package ch.admin.bj.swiyu.core.business.modules.trust.api;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record TrustAdditionalDidsSubmissionUpdateRequestDto(@NotNull List<String> proofOfPossessions) {}
