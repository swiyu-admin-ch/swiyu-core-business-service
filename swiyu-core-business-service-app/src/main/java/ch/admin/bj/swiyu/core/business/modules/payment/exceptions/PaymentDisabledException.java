package ch.admin.bj.swiyu.core.business.modules.payment.exceptions;

import ch.admin.bj.swiyu.core.business.common.exceptions.BusinessException;
import ch.admin.bj.swiyu.core.business.common.exceptions.BusinessExceptionErrorCode;

/**
 * The payment feature (EIDARTFE_1297_PAYMENT) is disabled on this stage.
 */
public class PaymentDisabledException extends BusinessException {

    public PaymentDisabledException() {
        super("Payment feature is disabled.");
    }

    @Override
    public BusinessExceptionErrorCode getErrorCode() {
        return BusinessExceptionErrorCode.PAYMENT_DISABLED;
    }
}
