package ch.admin.bj.swiyu.core.business.common.domain;

import java.util.Locale;

public enum Language {
    EN,
    DE,
    FR,
    IT,
    RM;

    public Locale getSwissLocale() {
        return switch (this) {
            case EN -> Locale.of("en", "CH");
            case DE -> Locale.of("de", "CH");
            case FR -> Locale.of("fr", "CH");
            case IT -> Locale.of("it", "CH");
            case RM -> Locale.of("rm", "CH");
        };
    }
}
