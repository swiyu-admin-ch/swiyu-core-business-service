package ch.admin.bj.swiyu.core.business.modules.payment.exceptions;

import ch.admin.bj.swiyu.core.business.common.exceptions.BusinessException;
import ch.admin.bj.swiyu.core.business.common.exceptions.BusinessExceptionErrorCode;
import lombok.Getter;

/**
 * A payment could not be initiated. The error code tells the caller why, e.g. PAYMENT_DISABLED,
 * PAYMENT_ALREADY_PENDING or PAYMENT_RETURN_URL_NOT_ALLOWED.
 */
public class PaymentInitializationFailedException extends BusinessException {

    @Getter
    private final BusinessExceptionErrorCode errorCode;

    public PaymentInitializationFailedException(BusinessExceptionErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
