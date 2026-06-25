package ch.admin.bj.swiyu.core.business.common.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Language", enumAsRef = true)
public enum LanguageDto {
    EN,
    DE,
    FR,
    IT,
    RM,
}
