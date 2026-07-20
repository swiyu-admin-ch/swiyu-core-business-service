package ch.admin.bj.swiyu.core.business.test;

import ch.admin.bj.swiyu.core.business.modules.documents.domain.PartnerDocumentsRepository;
import ch.admin.bj.swiyu.core.business.modules.identifier.domain.IdentifierEntryRepository;
import ch.admin.bj.swiyu.core.business.modules.management.domain.BusinessPartnerRepository;
import ch.admin.bj.swiyu.core.business.modules.status.domain.StatusListEntryRepository;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustAdditionalDidsSubmissionRepository;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.onboarding.TrustOnboardingSubmissionRepository;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.vcschema.VcSchemaSubmissionRepository;
import ch.admin.bj.swiyu.core.business.modules.trust.domain.vqps.VqpsSubmissionRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.test.context.transaction.TestTransaction;

/**
 * Convenience class for Integration Tests for the following reasons:
 *
 * <ul>
 *     <li>Simple access for cleaning data in between tests and commiting transactions</li>
 *     <li>Simple access to a repo (inject only this one)</li>
 * </ul>
 */
@Component
@AllArgsConstructor
public class TestRepositories {

    public final IdentifierEntryRepository identifierEntry;
    public final BusinessPartnerRepository businessPartner;
    public final VcSchemaSubmissionRepository vcSchemaSubmission;
    public final TrustOnboardingSubmissionRepository trustOnboardingSubmission;
    public final VqpsSubmissionRepository vqpsSubmission;
    public final TrustAdditionalDidsSubmissionRepository trustAdditionalDidsSubmission;
    public final PartnerDocumentsRepository partnerDocuments;
    public final StatusListEntryRepository statusListEntry;

    /**
     * Truncates all tables.
     */
    public void truncateTables() {
        var activeTransaction = TestTransaction.isActive();
        if (activeTransaction) {
            // first commit, since in spring each test starts with an open transaction
            commit();
        }
        // now each delete in separate transaction
        partnerDocuments.deleteAllInBatch();
        identifierEntry.deleteAllInBatch();
        businessPartner.deleteAllInBatch();
        vcSchemaSubmission.deleteAllInBatch();
        trustOnboardingSubmission.deleteAllInBatch();
        trustAdditionalDidsSubmission.deleteAllInBatch();
        vqpsSubmission.deleteAllInBatch();
        statusListEntry.deleteAllInBatch();
        if (activeTransaction) {
            // so all tests start with an open transaction
            startNewTransaction();
        }
    }

    /**
     * Opens a new Transaction. Might be useful if there are multiple transactions are needed within a test.
     */
    public void startNewTransaction() {
        TestTransaction.start();
    }

    public void commit() {
        TestTransaction.flagForCommit();
        TestTransaction.end();
    }
}
