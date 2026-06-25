package ch.admin.bj.swiyu.core.business.common.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "MultiLanguageText")
public record MultiLanguageTextDto(String de, String fr, String it, String en, String rm) {}
