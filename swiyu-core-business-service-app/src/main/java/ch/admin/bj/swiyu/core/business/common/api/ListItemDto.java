package ch.admin.bj.swiyu.core.business.common.api;

import ch.admin.bj.swiyu.core.business.common.api.utils.PageableUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

/**
 * Used as basis Dto for list REST endpoints.
 * <p>
 * Is utilized by PageableUtils to enforce default use cases for listing endpoints
 *
 * @see PageableUtils
 */
public interface ListItemDto {
    @Schema
    @NotNull
    UUID id();

    @Schema(example = "2024-10-29T09:35:16.809924Z")
    @NotNull
    Instant createdAt();

    @Schema(example = "2024-10-29T09:35:16.809924Z")
    @NotNull
    Instant updatedAt();
}
