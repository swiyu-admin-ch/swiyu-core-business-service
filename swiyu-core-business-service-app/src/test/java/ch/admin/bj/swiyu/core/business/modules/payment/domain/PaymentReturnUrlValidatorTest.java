package ch.admin.bj.swiyu.core.business.modules.payment.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.admin.bj.swiyu.core.business.common.exceptions.BusinessExceptionErrorCode;
import ch.admin.bj.swiyu.core.business.modules.payment.config.PaymentProperties;
import ch.admin.bj.swiyu.core.business.modules.payment.exceptions.PaymentInitializationFailedException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PaymentReturnUrlValidatorTest {

    private final PaymentReturnUrlValidator validator = new PaymentReturnUrlValidator(
        new PaymentProperties(
            List.of("https://portal.example.local", "https://portal.example.local:8443"),
            new PaymentProperties.SaferpayProperties("t", "c", URI.create("https://test.saferpay.com/api"), null),
            List.of(new PaymentProperties.Product("BASE_REGISTRATION", "Base", 15_000))
        )
    );

    @ParameterizedTest
    @ValueSource(
        strings = {
            "https://portal.example.local/callback",
            "https://portal.example.local/other/path?x=1",
            "https://portal.example.local:8443/callback",
            "https://portal.example.local:443/callback", // explicit default https port vs. implicit config
        }
    )
    void validate_matchingOrigin_doesNotThrow(String url) throws MalformedURLException {
        var returnUrl = URI.create(url).toURL();

        assertThatCode(() -> validator.validate(returnUrl)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "https://evil.example.com/callback",
            "http://portal.example.local/callback", // wrong scheme
            "https://portal.example.local:9999/callback", // wrong port
        }
    )
    void validate_nonMatchingOrigin_throws(String url) throws MalformedURLException {
        var returnUrl = URI.create(url).toURL();

        assertThatThrownBy(() -> validator.validate(returnUrl))
            .isInstanceOf(PaymentInitializationFailedException.class)
            .hasFieldOrPropertyWithValue("errorCode", BusinessExceptionErrorCode.PAYMENT_RETURN_URL_NOT_ALLOWED);
    }

    @Test
    void validate_noAllowedOrigins_throws() throws MalformedURLException {
        var validatorWithNoOrigins = new PaymentReturnUrlValidator(
            new PaymentProperties(
                List.of(),
                new PaymentProperties.SaferpayProperties("t", "c", URI.create("https://test.saferpay.com/api"), null),
                List.of(new PaymentProperties.Product("BASE_REGISTRATION", "Base", 15_000))
            )
        );

        var returnUrl = URI.create("https://portal.example.local/callback").toURL();

        assertThatThrownBy(() -> validatorWithNoOrigins.validate(returnUrl))
            .isInstanceOf(PaymentInitializationFailedException.class)
            .hasFieldOrPropertyWithValue("errorCode", BusinessExceptionErrorCode.PAYMENT_RETURN_URL_NOT_ALLOWED);
    }
}
