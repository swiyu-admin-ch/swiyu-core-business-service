package ch.admin.bj.swiyu.core.business.common.exceptions;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

public abstract class BusinessException extends RuntimeException {

    @Getter
    private final List<String> additionalDetails;

    protected BusinessException(String message) {
        super(message);
        this.additionalDetails = new ArrayList<>();
    }

    protected BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.additionalDetails = new ArrayList<>();
    }

    protected BusinessException(String message, String additionalDetails, Throwable cause) {
        super(message, cause);
        this.additionalDetails = new ArrayList<>();
        this.additionalDetails.add(additionalDetails);
    }

    protected BusinessException(String message, List<String> additionalDetails, Throwable cause) {
        super(message, cause);
        this.additionalDetails = additionalDetails;
    }

    public abstract BusinessExceptionErrorCode getErrorCode();
}
