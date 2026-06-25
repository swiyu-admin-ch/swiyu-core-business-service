package ch.admin.bj.swiyu.core.business.modules.trust.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.Builder;

@Builder
@Schema(name = "ProofOfPossessionSubmission")
public record ProofOfPossessionSubmissionDto(@Schema @NotBlank List<String> proofOfPossessions) {}
