package ch.admin.bj.swiyu.core.business.common.audit;

import ch.admin.bit.jeap.audit.command.builder.CreateAuditRecordCommandBuilder;

/**
 * Who caused an audited action, when it is not the caller of the current request. The hard delete is
 * executed by a Kafka consumer but ordered by a person in TMS, who sends themselves along in the
 * command payload; everything else derives the trigger from the security context instead.
 */
public sealed interface AuditTrigger {
    void applyTo(CreateAuditRecordCommandBuilder builder);

    record User(String id, String identityProvider) implements AuditTrigger {
        @Override
        public void applyTo(CreateAuditRecordCommandBuilder builder) {
            builder.setTriggerUser(id, identityProvider);
        }
    }

    record SystemComponent(String department, String system, String component) implements AuditTrigger {
        @Override
        public void applyTo(CreateAuditRecordCommandBuilder builder) {
            builder.setTriggerSystem(department, system, component);
        }
    }
}
