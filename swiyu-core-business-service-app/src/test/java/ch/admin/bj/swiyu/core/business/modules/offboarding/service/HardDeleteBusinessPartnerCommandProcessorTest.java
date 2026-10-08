package ch.admin.bj.swiyu.core.business.modules.offboarding.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.admin.bj.swiyu.core.business.common.audit.AuditTrigger;
import ch.admin.bj.swiyu.core.business.common.exceptions.HardDeleteNotAllowedException;
import ch.admin.bj.swiyu.messagetype.ti.TiHardDeleteBusinessPartnerCommand;
import ch.admin.bj.swiyu.messagetype.ti.TiHardDeleteBusinessPartnerCommandPayload;
import ch.admin.bj.swiyu.messagetype.ti.common.AuditSystemComponent;
import ch.admin.bj.swiyu.messagetype.ti.common.AuditUser;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HardDeleteBusinessPartnerCommandProcessorTest {

    private static final UUID PARTNER_ID = UUID.randomUUID();
    private static final String USER_ID = "bj-user-1";
    private static final String IDENTITY_PROVIDER = "https://idp.example.com";

    private OffboardingService offboardingService;
    private HardDeleteBusinessPartnerCommandProcessor processor;

    @BeforeEach
    void setUp() {
        offboardingService = mock(OffboardingService.class);
        processor = new HardDeleteBusinessPartnerCommandProcessor(offboardingService);
    }

    @Test
    void process_passesTheUserFromTheCommandOnAsTheAuditTrigger() {
        processor.process(command(auditUser()));

        verify(offboardingService).hardDeleteBusinessPartner(
            PARTNER_ID,
            new AuditTrigger.User(USER_ID, IDENTITY_PROVIDER)
        );
    }

    @Test
    void process_passesTheSystemComponentFromTheCommandOnAsTheAuditTrigger() {
        var trigger = AuditSystemComponent.newBuilder()
            .setDepartment("BJ")
            .setSystem("ti")
            .setComponent("swiyu-trust-management-scs")
            .build();

        processor.process(command(trigger));

        verify(offboardingService).hardDeleteBusinessPartner(
            PARTNER_ID,
            new AuditTrigger.SystemComponent("BJ", "ti", "swiyu-trust-management-scs")
        );
    }

    /** Without a trigger the audit record could not name who ordered the deletion, so nothing is deleted. */
    @Test
    void process_refusesACommandWithoutATrigger() {
        var command = command(null);

        assertThatThrownBy(() -> processor.process(command)).isInstanceOf(IllegalArgumentException.class);

        verify(offboardingService, never()).hardDeleteBusinessPartner(any(), any());
    }

    @Test
    void process_refusesACommandWithAnUnknownTriggerType() {
        var command = command("not-a-known-union-branch");

        assertThatThrownBy(() -> processor.process(command)).isInstanceOf(IllegalArgumentException.class);

        verify(offboardingService, never()).hardDeleteBusinessPartner(any(), any());
    }

    /**
     * The processor classifies nothing: a refused safeguard reaches the error handling service unchanged,
     * and ops resends the command once the partner is armed.
     */
    @Test
    void process_letsARefusedSafeguardPropagate() {
        doThrow(new HardDeleteNotAllowedException(PARTNER_ID))
            .when(offboardingService)
            .hardDeleteBusinessPartner(any(), any());
        var command = command(auditUser());

        assertThatThrownBy(() -> processor.process(command)).isInstanceOf(HardDeleteNotAllowedException.class);
    }

    @Test
    void process_letsAPartialFailurePropagate() {
        doThrow(new IllegalStateException("database is gone"))
            .when(offboardingService)
            .hardDeleteBusinessPartner(any(), any());
        var command = command(auditUser());

        assertThatThrownBy(() -> processor.process(command))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("database is gone");
    }

    private static AuditUser auditUser() {
        return AuditUser.newBuilder().setId(USER_ID).setIdentityProvider(IDENTITY_PROVIDER).build();
    }

    private static TiHardDeleteBusinessPartnerCommand command(Object trigger) {
        var payload = new TiHardDeleteBusinessPartnerCommandPayload();
        payload.setTrigger(trigger);
        payload.setBusinessPartnerId(PARTNER_ID);
        var command = mock(TiHardDeleteBusinessPartnerCommand.class);
        when(command.getPayload()).thenReturn(payload);
        return command;
    }
}
