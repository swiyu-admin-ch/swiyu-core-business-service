package ch.admin.bj.swiyu.core.business.common.audit;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public class AuditMapper {

    // findAndRegisterModules registers JavaTimeModule for Instant serialization
    // WRITE_DATES_AS_TIMESTAMPS disabled to produce ISO-8601 strings instead of numbers
    private static final ObjectMapper MAPPER = JsonMapper.builder().findAndAddModules().build();

    private AuditMapper() {}

    public static String toAuditJson(Object object) {
        try {
            return MAPPER.writeValueAsString(object);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to serialize object for audit", e);
        }
    }
}
