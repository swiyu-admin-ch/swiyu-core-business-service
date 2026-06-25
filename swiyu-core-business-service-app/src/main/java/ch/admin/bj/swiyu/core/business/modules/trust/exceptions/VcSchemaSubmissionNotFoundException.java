package ch.admin.bj.swiyu.core.business.modules.trust.exceptions;

import ch.admin.bj.swiyu.core.business.common.exceptions.BusinessException;
import ch.admin.bj.swiyu.core.business.common.exceptions.BusinessExceptionErrorCode;
import jakarta.annotation.Nullable;

public class VcSchemaSubmissionNotFoundException extends BusinessException {

    public VcSchemaSubmissionNotFoundException(String vcSchemaSubmissionId, @Nullable Throwable cause) {
        super(String.format("VcSchemaSubmission with id '%s' not found.", vcSchemaSubmissionId), cause);
    }

    @Override
    public BusinessExceptionErrorCode getErrorCode() {
        return BusinessExceptionErrorCode.VC_SCHEMA_SUBMISSION_NOT_FOUND;
    }
}
