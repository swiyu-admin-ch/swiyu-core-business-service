package ch.admin.bj.swiyu.core.business.modules.trust.api;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @param fileReport  The serialized FileReport from Discrete Validator as JSON.
 */
public record DeclarationOfIntentValidationResult(JsonNode fileReport) {}
