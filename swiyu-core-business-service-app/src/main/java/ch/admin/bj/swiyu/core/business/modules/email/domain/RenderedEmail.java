package ch.admin.bj.swiyu.core.business.modules.email.domain;

/**
 * A fully composed email: the subject line carrying all four languages, and the plain text body
 * containing the DE / FR / IT / EN sections.
 */
public record RenderedEmail(String subject, String plainTextMessage) {}
