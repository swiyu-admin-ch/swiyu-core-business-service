package ch.admin.bj.swiyu.core.business.common.service.mapper;

import ch.admin.bj.swiyu.core.business.common.domain.Language;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LanguageMapper {

    public static Language fromLanguageTag(String languageTag) {
        if (languageTag == null || languageTag.isBlank()) {
            return Language.EN;
        }
        return fromLocale(Locale.forLanguageTag(languageTag));
    }

    public static Language fromLocale(Locale locale) {
        if (locale == null) {
            return Language.EN;
        }
        return switch (locale.getLanguage()) {
            case "de" -> Language.DE;
            case "fr" -> Language.FR;
            case "it" -> Language.IT;
            case "rm" -> Language.RM;
            default -> Language.EN;
        };
    }
}
