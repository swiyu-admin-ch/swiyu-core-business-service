package ch.admin.bj.swiyu.core.business.common.api.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ch.admin.bj.swiyu.core.business.common.exceptions.InvalidPaginationApiException;
import ch.admin.bj.swiyu.core.business.modules.status.api.StatusListEntryDto;
import ch.admin.bj.swiyu.core.business.modules.status.domain.StatusListEntry;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

class PageableUtilsTest {

    private static final Class<StatusListEntryDto> testListItemDtoClass = StatusListEntryDto.class;
    private static final Class<?> testListItemClass = StatusListEntry.class;

    @Test
    void fromUserPageableForTrustOnboardingSubmission_whenValidSort_thenMapped() {
        // given
        var pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "id", "createdAt", "updatedAt"));

        // when
        var result = PageableUtils.toDbPageableFromUserPageable(testListItemDtoClass, testListItemClass, pageable);

        // then
        assertThat(result.getPageNumber()).isEqualTo(pageable.getPageNumber());
        assertThat(result.getPageSize()).isEqualTo(pageable.getPageSize());
        assertThat(result.getSort().getOrderFor("id")).isNotNull();
    }

    @Test
    void fromUserPageableForTrustOnboardingSubmission_whenInvalidSort_thenThrowsException() {
        // given
        var pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "invalidField"));

        // when / then
        assertThrows(InvalidPaginationApiException.class, () ->
            PageableUtils.toDbPageableFromUserPageable(testListItemDtoClass, testListItemClass, pageable)
        );
    }
}
