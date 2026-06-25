package ch.admin.bj.swiyu.core.business.common.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class AuditMapper {

    // findAndRegisterModules registers JavaTimeModule for Instant serialization
    // WRITE_DATES_AS_TIMESTAMPS disabled to produce ISO-8601 strings instead of numbers
    private static final ObjectMapper MAPPER = new ObjectMapper()
        .findAndRegisterModules()
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private AuditMapper() {}

    public static String toAuditJson(Object object) {
        try {
            return MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize object for audit", e);
        }
    }
}
