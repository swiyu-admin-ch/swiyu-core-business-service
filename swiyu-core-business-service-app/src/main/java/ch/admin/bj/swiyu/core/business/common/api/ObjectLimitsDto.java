package ch.admin.bj.swiyu.core.business.common.api;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(
    name = "ObjectLimits",
    description = """
    This shows the limits which are enacted for a given object category.
    """
)
public record ObjectLimitsDto(ApiObjectDto relatesTo, Long currentCount, Long maxCount) {}
