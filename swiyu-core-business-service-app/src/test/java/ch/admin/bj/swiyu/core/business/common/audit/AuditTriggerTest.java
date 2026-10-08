package ch.admin.bj.swiyu.core.business.common.audit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import ch.admin.bit.jeap.audit.command.builder.CreateAuditRecordCommandBuilder;
import org.junit.jupiter.api.Test;

class AuditTriggerTest {

    private final CreateAuditRecordCommandBuilder builder = mock(CreateAuditRecordCommandBuilder.class);

    @Test
    void user_isWrittenAsTheTriggeringUser() {
        new AuditTrigger.User("bj-user-1", "https://idp.example.com").applyTo(builder);

        verify(builder).setTriggerUser("bj-user-1", "https://idp.example.com");
        verifyNoMoreInteractions(builder);
    }

    @Test
    void systemComponent_isWrittenAsTheTriggeringSystem() {
        new AuditTrigger.SystemComponent("BJ", "ti", "swiyu-trust-management-scs").applyTo(builder);

        verify(builder).setTriggerSystem("BJ", "ti", "swiyu-trust-management-scs");
        verifyNoMoreInteractions(builder);
    }
}
