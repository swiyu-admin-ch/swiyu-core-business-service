package ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;

public record DeclarationOfIntent(@NotNull String fullySignedDocumentId, JsonNode validationReport) {}
