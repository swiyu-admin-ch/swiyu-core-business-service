package ch.admin.bj.swiyu.discrete.validator;

import org.springframework.web.multipart.MultipartFile;

/**
 * Client interface for executing signature validation requests.
 */
public interface DiscreteValidatorClient {
    /**
     * Executes a validation request for the given file, client, and expected amount of signatories.
     *
     * @param file the file to be validated
     * @param client the client requesting the validation
     * @param expectedAmountOfSignatories the expected number of signatory reports to receive (needed so we can return
     *                                    a valid result when mocking this client)
     * @return the result of the validation
     */
    DiscreteValidationResult executeValidationRequest(
        MultipartFile file,
        String client,
        int expectedAmountOfSignatories
    );
}
