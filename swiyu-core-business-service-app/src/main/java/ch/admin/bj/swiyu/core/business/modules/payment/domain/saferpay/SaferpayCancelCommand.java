package ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay;

import ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.model.SaferpayTransactionReference;

public record SaferpayCancelCommand(SaferpayTransactionReference transactionReference) {}
