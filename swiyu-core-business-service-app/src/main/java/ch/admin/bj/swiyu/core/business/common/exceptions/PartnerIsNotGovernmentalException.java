package ch.admin.bj.swiyu.core.business.common.exceptions;

/**
 * Thrown when non-governmental partners try to submit a VcSchema.
 */
public class PartnerIsNotGovernmentalException extends BusinessException {

    public PartnerIsNotGovernmentalException(String message) {
        super(message);
    }

    @Override
    public BusinessExceptionErrorCode getErrorCode() {
        return BusinessExceptionErrorCode.PARTNER_IS_NOT_GOVERNMENTAL;
    }
}
