package ch.admin.bj.swiyu.core.business.common.exceptions;

import java.util.UUID;
import lombok.Getter;

@Getter
public class VqpsPublicationTimeoutException extends BusinessException {

    private final UUID vqpsSubmissionId;

    public VqpsPublicationTimeoutException(UUID vqpsSubmissionId, Throwable cause) {
        super(
            "The processing of the vqps publication took longer than the configured max time. Pending request will be aborted.",
            cause
        );
        this.vqpsSubmissionId = vqpsSubmissionId;
    }

    @Override
    public BusinessExceptionErrorCode getErrorCode() {
        return BusinessExceptionErrorCode.VQPS_PUBLICATION_WAIT_TIMEOUT;
    }
}
