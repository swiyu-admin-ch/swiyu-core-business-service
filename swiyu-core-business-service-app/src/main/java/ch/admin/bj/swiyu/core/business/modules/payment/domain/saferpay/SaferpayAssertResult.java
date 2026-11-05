package ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay;

import ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.model.SaferpayLiability;
import ch.admin.bj.swiyu.core.business.modules.payment.domain.saferpay.model.SaferpayTransaction;

public record SaferpayAssertResult(SaferpayTransaction transaction, SaferpayLiability liability) {}
