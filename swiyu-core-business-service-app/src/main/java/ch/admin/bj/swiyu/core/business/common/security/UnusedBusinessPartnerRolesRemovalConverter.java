package ch.admin.bj.swiyu.core.business.common.security;

import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.jwt.MappedJwtClaimSetConverter;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component("unusedBusinessPartnerRoleRemovalConverter")
public class UnusedBusinessPartnerRolesRemovalConverter implements Converter<Map<String, Object>, Map<String, Object>> {

    private static final String ROLE_TO_FILTER = "apimgmt%selfservice";

    private final Converter<Map<String, Object>, Map<String, Object>> defaultConverter =
        MappedJwtClaimSetConverter.withDefaults(emptyMap());

    @Override
    public @NonNull Map<@NonNull String, @NonNull Object> convert(@NonNull Map<String, Object> claims) {
        var mappedClaims = new HashMap<>(claims);
        // TEMPORARY WORKAROUND:
        // Remove the known incompatible external role before JEAP processes the claims.
        // JEAP semantic role parsing currently fails on this role because it contains
        // JEAP's separator character in a format JEAP does not support yet.
        filterProblematicRoleFromBpRoles(mappedClaims);

        return requireNonNull(defaultConverter.convert(mappedClaims));
    }

    private void filterProblematicRoleFromBpRoles(Map<String, Object> mappedClaims) {
        var bpRolesObj = mappedClaims.get("bproles");
        if (!(bpRolesObj instanceof Map<?, ?> rawMap)) {
            return;
        }

        var changed = false;
        Map<String, Object> filteredBpRoles = new HashMap<>();

        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                filteredBpRoles.put(String.valueOf(entry.getKey()), entry.getValue());
                continue;
            }

            Object value = entry.getValue();
            if (value instanceof List<?> rolesList) {
                List<String> filteredRoles = rolesList
                    .stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .filter(role -> !ROLE_TO_FILTER.equals(role))
                    .toList();

                if (filteredRoles.size() != rolesList.size()) {
                    changed = true;
                }

                filteredBpRoles.put(key, filteredRoles);
            } else {
                filteredBpRoles.put(key, value);
            }
        }

        if (changed) {
            log.warn(
                "Temporary workaround active: filtered incompatible role '{}' from 'bproles' claim before JEAP processing.",
                ROLE_TO_FILTER
            );
            mappedClaims.put("bproles", filteredBpRoles);
        }
    }
}
