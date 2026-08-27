package ch.admin.bj.swiyu.core.business.modules.email.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SentNotificationRepository extends JpaRepository<SentNotification, UUID> {}
