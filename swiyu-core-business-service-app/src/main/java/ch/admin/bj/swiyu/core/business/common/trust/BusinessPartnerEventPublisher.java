package ch.admin.bj.swiyu.core.business.common.trust;

import java.util.UUID;

/**
 * Publishes the {@code TiBusinessPartner} change notifications through the transactional outbox.
 *
 * <p>One method per event, both carrying the partner id only. All methods must be called from within
 * an existing transaction: the event goes through the transactional outbox and must be committed
 * together with the state change that triggered it.
 *
 * <p>This interface deliberately lives in {@code common} while its implementation lives in
 * {@code modules.trust.service}: every module may depend on {@code common}, so the trigger points
 * couple to the abstraction only and no module to module dependency arises.
 */
public interface BusinessPartnerEventPublisher {
    /**
     * A thin change notification, also used for the DevOps sync triggers (EID-6988). Must not be
     * called for {@code BusinessPartnerIdentity} changes - those originate from TMS and would be
     * echoed back.
     */
    void businessPartnerUpdated(UUID businessPartnerId);

    /**
     * Signals that the partner has been hard deleted and its data must be purged downstream.
     */
    void businessPartnerHardDeleted(UUID businessPartnerId);
}
