package ch.admin.bj.swiyu.core.business.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class UnusedBusinessPartnerRolesRemovalConverterTest {

    private final UnusedBusinessPartnerRolesRemovalConverter converter =
        new UnusedBusinessPartnerRolesRemovalConverter();

    @Test
    void shouldRemoveProblematicRoleFromBusinessPartnerRoles() {
        Map<String, Object> claims = Map.of(
            "sub",
            "user-123",
            "bproles",
            Map.of(
                "bp-1",
                List.of("ti_@businesspartner_#read", "apimgmt%selfservice"),
                "bp-2",
                List.of("ti_@identifier_#write")
            )
        );

        Map<String, Object> result = converter.convert(claims);

        assertThat(result).containsKey("bproles");

        Assertions.assertNotNull(result);
        @SuppressWarnings("unchecked")
        Map<String, Object> bpRoles = (Map<String, Object>) result.get("bproles");

        assertThat(bpRoles)
            .containsEntry("bp-1", List.of("ti_@businesspartner_#read"))
            .containsEntry("bp-2", List.of("ti_@identifier_#write"));
    }

    @Test
    void shouldLeaveClaimsUntouchedWhenProblematicRoleIsAbsent() {
        Map<String, Object> claims = Map.of(
            "sub",
            "user-123",
            "bproles",
            Map.of("bp-1", List.of("ti_@businesspartner_#read"), "bp-2", List.of("ti_@identifier_#write"))
        );

        Map<String, Object> result = converter.convert(claims);

        Assertions.assertNotNull(result);
        @SuppressWarnings("unchecked")
        Map<String, Object> bpRoles = (Map<String, Object>) result.get("bproles");

        assertThat(bpRoles)
            .containsEntry("bp-1", List.of("ti_@businesspartner_#read"))
            .containsEntry("bp-2", List.of("ti_@identifier_#write"));
    }

    @Test
    void shouldLeaveClaimsUntouchedWhenBusinessPartnerRolesClaimIsMissing() {
        Map<String, Object> claims = Map.of("sub", "user-123");

        Map<String, Object> result = converter.convert(claims);

        assertThat(result).containsEntry("sub", "user-123").doesNotContainKey("bproles");
    }

    @Test
    void shouldLeaveClaimsUntouchedWhenBusinessPartnerRolesClaimIsNotAMap() {
        Map<String, Object> claims = Map.of("sub", "user-123", "bproles", "not-a-map");

        Map<String, Object> result = converter.convert(claims);

        assertThat(result).containsEntry("bproles", "not-a-map");
    }

    @Test
    void shouldIgnoreNonStringEntriesInRoleList() {
        Map<String, Object> claims = Map.of(
            "sub",
            "user-123",
            "bproles",
            Map.of("bp-1", List.of("ti_@businesspartner_#read", 123, "apimgmt%selfservice", true))
        );

        Map<String, Object> result = converter.convert(claims);

        Assertions.assertNotNull(result);
        @SuppressWarnings("unchecked")
        Map<String, Object> bpRoles = (Map<String, Object>) result.get("bproles");

        assertThat(bpRoles).containsEntry("bp-1", List.of("ti_@businesspartner_#read"));
    }

    @Test
    void shouldKeepNonListBusinessPartnerRoleValuesUntouched() {
        Map<String, Object> claims = Map.of("sub", "user-123", "bproles", Map.of("bp-1", "unexpected-value"));

        Map<String, Object> result = converter.convert(claims);

        Assertions.assertNotNull(result);
        @SuppressWarnings("unchecked")
        Map<String, Object> bpRoles = (Map<String, Object>) result.get("bproles");

        assertThat(bpRoles).containsEntry("bp-1", "unexpected-value");
    }
}
