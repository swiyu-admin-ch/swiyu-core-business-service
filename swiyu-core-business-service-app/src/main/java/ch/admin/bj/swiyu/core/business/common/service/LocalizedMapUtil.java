package ch.admin.bj.swiyu.core.business.common.service;

import static ch.admin.bj.swiyu.core.business.common.i18n.LocalizedMapConstants.DEFAULT_VALUE_KEY;

import ch.admin.bj.swiyu.core.business.common.domain.Language;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;
import org.springframework.util.StringUtils;

@UtilityClass
public class LocalizedMapUtil {

    public static String getByLanguageOrDefault(Map<String, String> map, Language language) {
        if (map == null) {
            return null;
        }
        return getOrFallback(map, language, getDefaultValue(map));
    }

    public static String getDefaultValue(Map<String, String> map) {
        if (map == null) {
            return null;
        }

        return map.get(DEFAULT_VALUE_KEY);
    }

    public static Map<String, String> fromSingleName(String name) {
        return Map.of(DEFAULT_VALUE_KEY, name);
    }

    public static Map<String, String> fromLanguages(
        String defaultValue,
        String de,
        String fr,
        String it,
        String en,
        String rm
    ) {
        var map = new LinkedHashMap<String, String>();
        map.put(DEFAULT_VALUE_KEY, defaultValue);
        map.put(Language.DE.getSwissLocale().toLanguageTag(), de);
        map.put(Language.FR.getSwissLocale().toLanguageTag(), fr);
        map.put(Language.IT.getSwissLocale().toLanguageTag(), it);
        map.put(Language.EN.getSwissLocale().toLanguageTag(), en);
        map.put(Language.RM.getSwissLocale().toLanguageTag(), rm);
        return Map.copyOf(map);
    }

    private static String getOrFallback(Map<String, String> map, Language language, String fallback) {
        var value = map.get(language.getSwissLocale().toLanguageTag());
        return StringUtils.hasText(value) ? value : fallback;
    }
}
