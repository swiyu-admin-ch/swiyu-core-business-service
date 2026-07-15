package ch.admin.bj.swiyu.discrete.validator;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Represents the result of a discrete validation.
 *
 * @param isValidStatus true, if the validation succeeded
 * @param fileReport the full file report as json
 * @param amountOfSignatures the number of signature reports which came within the file report.
 */
public record DiscreteValidationResult(boolean isValidStatus, JsonNode fileReport, int amountOfSignatures) {}
