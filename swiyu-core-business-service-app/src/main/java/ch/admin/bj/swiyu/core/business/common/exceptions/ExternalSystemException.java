package ch.admin.bj.swiyu.core.business.common.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;

@Getter
public class ExternalSystemException extends RuntimeException {

    private final ExternalSystem externalSystem;
    private final HttpStatusCode httpStatusCode;

    public ExternalSystemException(String message, ExternalSystem externalSystem, HttpStatusCode httpStatusCode) {
        super(String.format("Error when accessing external system '%s': %s", externalSystem, message));
        this.externalSystem = externalSystem;
        this.httpStatusCode = httpStatusCode;
    }

    public ExternalSystemException(
        String message,
        ExternalSystem externalSystem,
        HttpStatusCode httpStatusCode,
        Throwable cause
    ) {
        super(String.format("Error when accessing external system '%s': %s", externalSystem, message), cause);
        this.externalSystem = externalSystem;
        this.httpStatusCode = httpStatusCode;
    }
}
