package ch.admin.bj.swiyu.core.business.modules.email.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SentNotificationRepository extends JpaRepository<SentNotification, UUID> {
    /**
     * Whether that exact notification was already sent. Only meaningful for the composed business keys
     * of the scheduled reminders - the event driven emails use a generated UUID, which never repeats.
     */
    boolean existsByIdempotenceId(String idempotenceId);
}
