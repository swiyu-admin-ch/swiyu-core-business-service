package ch.admin.bj.swiyu.core.business.common.exceptions;

import java.util.UUID;

/**
 * The hard-delete safeguard refused the deletion. Deliberately an exception and not a silent no-op, so
 * that the command fails visibly and can be resent once ops has armed the partner.
 */
public class HardDeleteNotAllowedException extends RuntimeException {

    public HardDeleteNotAllowedException(UUID businessPartnerId) {
        super(
            "Hard delete of business partner '%s' is refused: hardDeleteAllowed is false.".formatted(businessPartnerId)
        );
    }
}
