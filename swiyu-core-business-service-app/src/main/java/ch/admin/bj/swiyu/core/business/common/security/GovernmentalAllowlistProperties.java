package ch.admin.bj.swiyu.core.business.common.security;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configures the ID of a Partner whose users are considered governmental users. Users
 * that have business partner roles for this partner, can onboard (create new business partners)
 * without payment.
 *
 * @param partnerId the business partner id of the "Swiyu Governmenta Allowlist" Busines partner.
 */
@Validated
@ConfigurationProperties(prefix = "app.governmental-allowlist")
public record GovernmentalAllowlistProperties(@NotNull UUID partnerId) {}
