package ch.admin.bj.swiyu.core.business.common.security;

import static org.springframework.util.CollectionUtils.isEmpty;

import ch.admin.bit.jeap.security.resource.semanticAuthentication.ServletSemanticAuthorization;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Wrapper around jeap's ServletSemanticAuthorization with certain convenience methods.
 */
@AllArgsConstructor
@Component
@Slf4j
public class AuthSupport {

    private final GovernmentalAllowlistProperties governmentalAllowlistProperties;
    private final ServletSemanticAuthorization jeapAuthorization;

    /**
     * Checks if the current user has the given role for all of the given partnerIds. Can be used with @PreAuthorize
     * like this:
     *
     * <pre>
     * {@code
     * @PostAuthorize("@authSupport.hasRoleForPartners('businesspartner', 'read', returnObject.content.![id])")
     * public PagedModel<BusinessPartnerDto> getBusinessPartners() { ... }
     * }
     * </pre>
     */
    public boolean hasRoleForPartners(String resource, String operation, Collection<UUID> partnerIds) {
        if (isEmpty(partnerIds)) {
            return true; // no content means nothing to authorize
        }
        for (var partnerId : partnerIds) {
            if (!jeapAuthorization.hasRoleForPartner(resource, operation, partnerId.toString())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get all partner ids as UUID for which the current user has at least one role for the given resource with the
     * given operation.
     */
    public List<UUID> getPartnerIdsForRole(String resource, String operation) {
        return jeapAuthorization.getPartnersForRole(resource, operation).stream().map(UUID::fromString).toList();
    }

    /**
     * Convenience lookup of partner id when in B2B context. It assumes that there is exactly one partner id for the
     * given resource and operation.
     */
    public UUID getPartnerIdForRole(String resource, String operation) {
        var partnerIds = getPartnerIdsForRole(resource, operation);
        if (isEmpty(partnerIds)) {
            throw new AccessDeniedException(
                "Expected single business partner id for role @%s_#%s but found none".formatted(resource, operation)
            );
        }
        if (partnerIds.size() > 1) {
            throw new IllegalStateException(
                (
                    "Expected single business partner id for role @%s_#%s but found multiple. This is not supported."
                ).formatted(resource, operation)
            );
        }
        return partnerIds.getFirst();
    }

    public boolean hasRoleForAllPartners(String resource, String operation) {
        return jeapAuthorization.hasRoleForAllPartners(resource, operation);
    }

    /**
     * Throws an AccessDeniedException if the current user does not have the given role for the given partnerId (or
     * does not have role for all partners).
     */
    public void validateHasRoleForPartner(String resource, String operation, UUID partnerId) {
        if (!jeapAuthorization.hasRoleForPartner(resource, operation, partnerId.toString())) {
            throw new AccessDeniedException("Authorization is missing role: @%s_#%s".formatted(resource, operation));
        }
    }

    /**
     * Returns true if the current user has the business partner role <code>@businesspartner_#write</code> for
     * the "Swiyu Governmental Allowlist" partner.
     */
    public boolean isGovernmentalAllowlistUser() {
        return jeapAuthorization.hasRoleForPartner(
            "businesspartner",
            "write",
            this.governmentalAllowlistProperties.partnerId().toString()
        );
    }
}
