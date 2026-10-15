package ch.admin.bj.swiyu.core.business.modules.payment.domain;

import java.security.SecureRandom;
import lombok.experimental.UtilityClass;

/**
 * Generates human-readable payment reference ids in the format {@code P-[8 uppercase hex chars]}
 * (e.g. {@code P-A3F8B2C1}). Used as the Saferpay {@code orderId} and shown on receipts.
 */
@UtilityClass
public class PaymentReferenceIdGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generate() {
        return "P-" + String.format("%08X", RANDOM.nextInt());
    }
}
