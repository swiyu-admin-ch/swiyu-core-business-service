package ch.admin.bj.swiyu.core.business.common.utils;

import java.util.Optional;
import java.util.UUID;
import lombok.experimental.UtilityClass;

@UtilityClass
public class UUIDUtil {

    public static Optional<UUID> optionalUUID(String uuidString) {
        try {
            return Optional.of(UUID.fromString(uuidString));
        } catch (IllegalArgumentException _) {
            return Optional.empty();
        }
    }
}
