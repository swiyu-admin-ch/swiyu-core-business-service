package ch.admin.bj.swiyu.core.business.common.exceptions;

import static ch.admin.bj.swiyu.core.business.common.exceptions.BusinessExceptionErrorCode.VQPS_PUBLICATION_FAILED;

import java.util.UUID;
import lombok.Getter;

@Getter
public class VqpsPublicationFailedException extends BusinessException {

    private final UUID vqpsSubmissionId;

    public VqpsPublicationFailedException(String msg, UUID vqpsSubmissionId) {
        super(msg);
        this.vqpsSubmissionId = vqpsSubmissionId;
    }

    @Override
    public BusinessExceptionErrorCode getErrorCode() {
        return VQPS_PUBLICATION_FAILED;
    }
}
