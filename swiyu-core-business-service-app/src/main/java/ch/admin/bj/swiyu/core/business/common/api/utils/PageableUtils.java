package ch.admin.bj.swiyu.core.business.common.api.utils;

import ch.admin.bj.swiyu.core.business.common.api.ListItemDto;
import ch.admin.bj.swiyu.core.business.common.exceptions.InvalidPaginationApiException;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@UtilityClass
public class PageableUtils {

    public static <S extends ListItemDto, T> Pageable toDbPageableFromUserPageable(
        Class<S> dtoSource,
        Class<T> entityTarget,
        Pageable pageable
    ) {
        return toDbPageableFromUserPageable(dtoSource, entityTarget, pageable, Map.of());
    }

    public static <S extends ListItemDto, T> Pageable toDbPageableFromUserPageable(
        Class<S> dtoSource,
        Class<T> entityTarget,
        Pageable pageable,
        Map<String, String> sortFieldRemappings
    ) {
        // Get all fields of the class TrustOnboardingSubmissionD
        var allowedSortFields = Arrays.stream(dtoSource.getRecordComponents()).map(RecordComponent::getName).toList();

        // filter Pageable for invalid sort fields to not fail them silently
        pageable
            .getSort()
            .stream()
            .filter(c -> !allowedSortFields.contains(c.getProperty()))
            .findFirst()
            .ifPresent(c -> {
                throw new InvalidPaginationApiException(
                    entityTarget,
                    String.format(
                        "Sort by field '%s' is not supported. Allowed fields: %s",
                        c.getProperty(),
                        allowedSortFields
                    ),
                    null
                );
            });

        // Map Pageable fields to actual DB entities
        List<Sort.Order> updatedSort = pageable
            .getSort()
            .stream()
            .map(order -> mapSortOrder(order, sortFieldRemappings))
            .toList();

        // Copy Pageable details over as a Pageable is immutable
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(updatedSort));
    }

    private static Sort.Order mapSortOrder(Sort.Order order, Map<String, String> sortFieldRemappings) {
        String mappedProperty = switch (order.getProperty()) {
            case "createdAt" -> "auditMetadata.createdAt";
            case "updatedAt" -> "auditMetadata.lastModifiedAt";
            default -> sortFieldRemappings.getOrDefault(order.getProperty(), order.getProperty());
        };
        if (mappedProperty.equals(order.getProperty())) {
            return order;
        }
        return new Sort.Order(order.getDirection(), mappedProperty);
    }
}
