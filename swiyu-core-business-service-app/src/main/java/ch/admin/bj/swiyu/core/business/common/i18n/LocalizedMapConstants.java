package ch.admin.bj.swiyu.core.business.common.i18n;

import lombok.experimental.UtilityClass;

/**
 * Constants for localized map handling, where maps use language tags as keys
 * and localized string values as values.
 */
@UtilityClass
public class LocalizedMapConstants {

    /**
     * The key used to store the default (fallback) value in a localized map.
     * This entry is required and is used when no locale-specific value is available.
     *
     * <p>Example:
     * <pre>{@code
     * {
     *   "default": "Purpose name",
     *   "de-CH": "Zweckname",
     *   "fr-CH": "Nom de l'objectif",
     *   "it-CH": "Nome dello scopo"
     * }
     * }</pre>
     */
    public static final String DEFAULT_VALUE_KEY = "default";
}
