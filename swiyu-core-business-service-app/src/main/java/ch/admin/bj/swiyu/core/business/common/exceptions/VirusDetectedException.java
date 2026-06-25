package ch.admin.bj.swiyu.core.business.common.exceptions;

public class VirusDetectedException extends BusinessException {

    private static final String RESOURCE_TYPE_MESSAGE = "Virus detected.";

    public VirusDetectedException() {
        super(RESOURCE_TYPE_MESSAGE, null);
    }

    @Override
    public BusinessExceptionErrorCode getErrorCode() {
        return BusinessExceptionErrorCode.DATA_INVALID_VIRUS_DETECTED;
    }
}
