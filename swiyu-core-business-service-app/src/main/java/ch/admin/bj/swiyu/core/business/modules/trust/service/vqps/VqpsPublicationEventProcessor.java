package ch.admin.bj.swiyu.core.business.modules.trust.service.vqps;

import static ch.admin.bj.swiyu.core.business.modules.trust.service.vqps.VqpsSubmissionMapper.toVqpsPublicationFailureReasonDto;

import ch.admin.bj.swiyu.messagetype.ti.TiVqpsPublicationFailedEvent;
import ch.admin.bj.swiyu.messagetype.ti.TiVqpsPublicationSucceededEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Processes VQPS publication result events from TMS.
 * Must NOT be @Transactional: the service methods commit their own transactions, after which
 * the awaiter is notified so waiting B2B requests receive the updated submission state.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VqpsPublicationEventProcessor {

    private final VqpsSubmissionService vqpsSubmissionService;
    private final VqpsPublicationAwaiter vqpsPublicationAwaiter;

    public void processVqpsPublicationSucceeded(TiVqpsPublicationSucceededEvent event) {
        log.info(
            "Processing TiVqpsPublicationSucceededEvent for submission id {}",
            event.getPayload().getVqpsSubmissionId()
        );
        var submissionId = event.getPayload().getVqpsSubmissionId();
        var jwt = event.getPayload().getVqps();
        vqpsSubmissionService.markAsPublicationSucceeded(submissionId, jwt);
        vqpsPublicationAwaiter.notifyVqpsPublicationProcessFinished(submissionId);
    }

    public void processVqpsPublicationFailed(TiVqpsPublicationFailedEvent event) {
        log.info(
            "Processing TiVqpsPublicationFailedEvent for submission id {}",
            event.getPayload().getVqpsSubmissionId()
        );
        var submissionId = event.getPayload().getVqpsSubmissionId();
        vqpsSubmissionService.markAsPublicationFailed(
            submissionId,
            toVqpsPublicationFailureReasonDto(event.getPayload().getFailureReason())
        );
        vqpsPublicationAwaiter.notifyVqpsPublicationProcessFinished(submissionId);
    }
}
