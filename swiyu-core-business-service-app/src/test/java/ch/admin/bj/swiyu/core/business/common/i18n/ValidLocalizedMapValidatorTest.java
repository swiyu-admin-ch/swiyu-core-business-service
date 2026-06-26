package ch.admin.bj.swiyu.core.business.common.i18n;

import static ch.admin.bj.swiyu.core.business.common.i18n.ValidLocalizedMapValidator.validateLocalizedMap;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.validation.ValidationException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ValidLocalizedMapValidatorTest {

    @Test
    void validateLocalizedMap_whenMapIsEmpty_throw_since_default_missing() {
        var map = new HashMap<String, String>();
        assertThatThrownBy(() -> validateLocalizedMap(map))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Map cannot be empty");
    }

    @Test
    void validateLocalizedMap_whenAllKeysAreValidLocales_thenNoExceptionThrown() {
        var map = Map.of("default", "default value", "de", "Deutsch", "fr", "Français", "it", "Italiano");
        assertThatNoException().isThrownBy(() -> validateLocalizedMap(map));
    }

    @Test
    void validateLocalizedMap_whenKeyIsInvalidLocale_thenIllegalArgumentExceptionThrown() {
        var map = Map.of("default", "default value", "cheese", "value");
        assertThatThrownBy(() -> validateLocalizedMap(map)).isInstanceOf(ValidationException.class);
    }

    @Test
    void validateLocalizedMap_whenOneKeyIsInvalidAmongValidOnes_thenIllegalArgumentExceptionThrown() {
        var map = Map.of("default", "default value", "de", "Deutsch", "cheese", "value");
        assertThatThrownBy(() -> validateLocalizedMap(map)).isInstanceOf(ValidationException.class);
    }

    @Test
    void validateLocalizedMap_missing_default_value() {
        var map = Map.of("de", "Deutsch");
        assertThatThrownBy(() -> validateLocalizedMap(map))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Map must contain a \"default\" language entry");
    }
}
