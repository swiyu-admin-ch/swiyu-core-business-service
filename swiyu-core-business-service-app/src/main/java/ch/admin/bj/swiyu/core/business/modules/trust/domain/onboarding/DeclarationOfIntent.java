package ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding;

import ch.admin.suis.validator.rest.to.response.FileReport;
import jakarta.validation.constraints.NotNull;

public record DeclarationOfIntent(@NotNull String fullySignedDocumentId, FileReport validationReport) {}
