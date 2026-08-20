package ch.admin.bj.swiyu.core.business.common.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Properties for enabling or disabling certain functionalities of the application.
 *
 * <p>Same mechanism as in swiyu-ecosystem-portal-scs: functionalities are named after what they do
 * and must be configured explicitly on every stage - a missing value fails the startup instead of
 * silently defaulting. This is distinct from the Jira numbered flags in
 * {@link ch.admin.bj.swiyu.core.business.common.features.FeaturesProperties}, which mark work in
 * progress and are removed once the corresponding story is rolled out.
 *
 * @param emailEnabled If disabled, no partner notification email is published
 */
@Validated
@ConfigurationProperties(prefix = "app.functionality")
public record FunctionalityProperties(@NotNull Boolean emailEnabled) {}
