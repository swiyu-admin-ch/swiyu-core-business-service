package ch.admin.bj.swiyu.core.business.modules.management.domain.pams;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatusCode;

public record PamsHealthResponseDto(
    @NotEmpty Boolean success,
    @NotNull HttpStatusCode statusCode,
    @NotNull PamsHealthResponseDataDto data
) {}
