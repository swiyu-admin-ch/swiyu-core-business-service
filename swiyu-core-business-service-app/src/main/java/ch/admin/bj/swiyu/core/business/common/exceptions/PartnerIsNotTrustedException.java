package ch.admin.bj.swiyu.core.business.common.exceptions;

/**
 * Thrown when a partner who is not (yet) trusted tries to submit a ProtectedVerificationSubmission.
 */
public class PartnerIsNotTrustedException extends BusinessException {

    public PartnerIsNotTrustedException(String message) {
        super(message);
    }

    @Override
    public BusinessExceptionErrorCode getErrorCode() {
        return BusinessExceptionErrorCode.PARTNER_IS_NOT_TRUSTED;
    }
}
