package ch.admin.bj.swiyu.core.business.modules.offboarding.service;

import ch.admin.bj.swiyu.core.business.common.audit.AuditTrigger;
import ch.admin.bj.swiyu.core.business.modules.documents.service.PartnerDocumentService;
import ch.admin.bj.swiyu.core.business.modules.identifier.service.IdentifierEntryService;
import ch.admin.bj.swiyu.core.business.modules.management.service.BusinessPartnerService;
import ch.admin.bj.swiyu.core.business.modules.status.service.StatusListEntryService;
import ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding.TrustAdditionalDidsService;
import ch.admin.bj.swiyu.core.business.modules.trust.service.onboarding.TrustOnboardingService;
import ch.admin.bj.swiyu.core.business.modules.trust.service.protectedverification.ProtectedVerificationSubmissionService;
import ch.admin.bj.swiyu.core.business.modules.trust.service.vcschema.VcSchemaSubmissionService;
import ch.admin.bj.swiyu.core.business.modules.trust.service.vqps.VqpsSubmissionService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Deletes a business partner and all its data across the core DB, both registry DBs, S3 and PAMS.
 *
 * <p>Three datasources and two external systems cannot share one transaction, so each aggregate is
 * deleted in its own, in the fixed order below. Every step is written to tolerate already being done,
 * so a redelivered command finishes what a failed run left behind.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OffboardingService {

    private final BusinessPartnerService businessPartnerService;
    private final PartnerDocumentService partnerDocumentService;
    private final StatusListEntryService statusListEntryService;
    private final IdentifierEntryService identifierEntryService;
    private final VqpsSubmissionService vqpsSubmissionService;
    private final VcSchemaSubmissionService vcSchemaSubmissionService;
    private final ProtectedVerificationSubmissionService protectedVerificationSubmissionService;
    private final TrustAdditionalDidsService trustAdditionalDidsService;
    private final TrustOnboardingService trustOnboardingService;

    public void hardDeleteBusinessPartner(UUID businessPartnerId, AuditTrigger trigger) {
        // The partner row goes last, so its absence means a previous run got all the way through.
        if (!businessPartnerService.businessPartnerExists(businessPartnerId)) {
            log.info("Business partner '{}' is already hard deleted, nothing to do", businessPartnerId);
            return;
        }
        businessPartnerService.validateHardDeleteAllowed(businessPartnerId);

        log.info("Starting hard delete of business partner '{}'", businessPartnerId);
        businessPartnerService.deleteFromPams(businessPartnerId);

        // Documents before the submissions: the declaration-of-intent FK points the other way and is
        // ON DELETE SET NULL, so deleting them first is what breaks the cycle.
        partnerDocumentService.hardDeleteByPartnerId(businessPartnerId, trigger);
        statusListEntryService.hardDeleteByPartnerId(businessPartnerId, trigger);
        identifierEntryService.hardDeleteByPartnerId(businessPartnerId, trigger);

        vqpsSubmissionService.hardDeleteByPartnerId(businessPartnerId);
        vcSchemaSubmissionService.hardDeleteByPartnerId(businessPartnerId);
        protectedVerificationSubmissionService.hardDeleteByPartnerId(businessPartnerId);
        trustAdditionalDidsService.hardDeleteByPartnerId(businessPartnerId);
        trustOnboardingService.hardDeleteByPartnerId(businessPartnerId);

        businessPartnerService.hardDeleteBusinessPartner(businessPartnerId, trigger);
        log.info("Hard delete of business partner '{}' completed", businessPartnerId);
    }
}
