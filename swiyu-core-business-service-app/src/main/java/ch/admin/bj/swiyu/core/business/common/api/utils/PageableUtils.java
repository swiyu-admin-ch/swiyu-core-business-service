package ch.admin.bj.swiyu.core.business.common.api.utils;

import ch.admin.bj.swiyu.core.business.common.api.ListItemDto;
import ch.admin.bj.swiyu.core.business.common.exceptions.InvalidPaginationApiException;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
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
            .map(order -> {
                String property = order.getProperty();
                return switch (property) {
                    case "createdAt" -> new Sort.Order(order.getDirection(), "auditMetadata.createdAt");
                    case "updatedAt" -> new Sort.Order(order.getDirection(), "auditMetadata.lastModifiedAt");
                    default -> order;
                };
            })
            .toList();

        // Copy Pageable details over as a Pageable is immutable
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(updatedSort));
    }
}
