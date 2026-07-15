package ch.admin.bj.swiyu.discrete.validator;

import static org.springframework.util.CollectionUtils.isEmpty;

import ch.admin.suis.client.core.service.IValidationServiceClient;
import ch.admin.suis.client.core.service.ValidationServiceClientBuilder;
import ch.admin.suis.client.core.service.to.FileRequest;
import ch.admin.suis.validator.rest.to.ValidStatus;
import ch.admin.suis.validator.rest.to.response.FileReport;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.intarsys.aaa.authenticate.impl.UserPasswordCredential;
import de.intarsys.tools.crypto.Secret;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.web.multipart.MultipartFile;

/**
 * Client for Signature Validator of BIT. See <a href="https://www.bit.admin.ch/de/der-diskrete-validator">...</a>.
 */
@Slf4j
public class DefaultDiscreteValidatorClient implements DiscreteValidatorClient {

    /** Owner read/write only — no permissions for group or others. */
    private static final String TEMP_FILE_PERMISSIONS = "rw-------";

    private static final String BC_PROVIDER_NAME = "BC";

    private final IValidationServiceClient service;
    private final ObjectMapper objectMapper;

    public DefaultDiscreteValidatorClient(
        ObjectMapper objectMapper,
        String serviceUrl,
        String username,
        String password
    ) {
        ensureBouncyCastleProviderRegistered();
        this.service = ValidationServiceClientBuilder.newBuilder()
            .serviceUrl(serviceUrl)
            .credential(new UserPasswordCredential(username, Secret.hide(password.toCharArray())))
            .build();
        this.objectMapper = objectMapper;
    }

    @Override
    public DiscreteValidationResult executeValidationRequest(
        MultipartFile file,
        String client,
        int expectedAmountOfSignatories
    ) {
        log.debug(
            "Executing validation request for file {} with client {} and expected amount of signatories {}",
            file.getOriginalFilename(),
            client,
            expectedAmountOfSignatories
        );
        Path tempFile = writeToTempFile(file);
        try {
            var validationResponse = service.validateOneRequest(
                // the list of files to validate
                List.of(new FileRequest(tempFile.toFile(), client)),
                // a flag specifying whether the validation service should generate and return a report in PDF format
                false,
                // the userOrganization property to pass to the validation service
                null,
                // the userOrganization property to pass to the validation service
                null,
                // the language property to pass to the validation service
                "de",
                // the pdfReportName property that should be echoed by the validation service
                null,
                //  a flag specifying whether the call should write details about the service input and results to the log
                false,
                // a flag specifying whether files not containing a signature should be processed by the validation service
                true
            );
            var fileReport = !isEmpty(validationResponse.getFileReports())
                ? validationResponse.getFileReports().getFirst()
                : null;
            var valid = validationResponse.isValid() == ValidStatus.VALID;
            var amountOfSignatures = amountOfSignatures(fileReport);
            var fileReportAsJson = toJsonNode(fileReport);
            return new DiscreteValidationResult(valid, fileReportAsJson, amountOfSignatures);
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new IllegalStateException(e);
        } finally {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException _) {
                // ignore — temp file cleanup failure does not affect validation result
            }
        }
    }

    private JsonNode toJsonNode(FileReport fileReport) {
        if (fileReport == null) {
            return null;
        }
        String jsonString;
        try {
            jsonString = objectMapper.writeValueAsString(fileReport);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("failed to serialize FileReport from Discrete Validator API to JSON", e);
        }
        try {
            return objectMapper.readTree(jsonString);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("failed to deserialize FileReport JSON to JsonNode", e);
        }
    }

    private int amountOfSignatures(FileReport fileReport) {
        if (fileReport == null || fileReport.getSignatureReports() == null) {
            return 0;
        }
        return fileReport.getSignatureReports().size();
    }

    private static void ensureBouncyCastleProviderRegistered() {
        if (Security.getProvider(BC_PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private static Path writeToTempFile(MultipartFile file) {
        log.debug("Writing file {} to temp file for validation request", file.getOriginalFilename());
        try {
            var tempfile = Files.createTempFile(
                "doi-upload-",
                ".pdf",
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString(TEMP_FILE_PERMISSIONS))
            );
            Files.write(tempfile, file.getBytes());
            return tempfile;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write DOI File as temp file. Validation can't be completed.", e);
        }
    }
}
