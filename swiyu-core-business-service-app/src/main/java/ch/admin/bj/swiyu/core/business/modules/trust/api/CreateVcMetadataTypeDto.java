package ch.admin.bj.swiyu.core.business.modules.trust.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "CreateVcMetadata")
public record CreateVcMetadataTypeDto(
    @NotBlank
    @Schema(
        description = "JSON of VcMetadataType in String format.",
        example = "\"{\\n    \\\"vct\\\": \\\"https://registry-url.ch/eid-v1\\\",\\n    \\\"name\\\": \\\"XYZ Credential\\\",\\n    \\\"description\\\": \\\"\\\",\\n    \\\"extends\\\": \\\"\\\",\\n    \\\"extends#integrity\\\": \\\"\\\",\\n    \\\"schema_uri\\\": \\\"https://url-to-json-schema-which-defines-type\\\"\\n}\""
    )
    String vcTypeMetadata
) {}
