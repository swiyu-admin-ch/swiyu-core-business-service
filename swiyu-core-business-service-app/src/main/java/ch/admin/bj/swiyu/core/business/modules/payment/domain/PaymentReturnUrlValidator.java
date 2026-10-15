package ch.admin.bj.swiyu.core.business.modules.payment.domain;

import ch.admin.bj.swiyu.core.business.common.exceptions.BusinessExceptionErrorCode;
import ch.admin.bj.swiyu.core.business.modules.payment.config.PaymentProperties;
import ch.admin.bj.swiyu.core.business.modules.payment.exceptions.PaymentInitializationFailedException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Security check for the returnUrl of a payment. The caller supplies the URL the browser is redirected to
 * after the Saferpay payment page. Without this check a caller could pass any site, and the user would be
 * sent there straight from the genuine Saferpay page (open redirect, e.g. usable for phishing). Therefore
 * only URLs whose origin (scheme, host, port) is listed in {@code payment.allowed-return-origins} are
 * accepted.
 */
@Service
@RequiredArgsConstructor
public class PaymentReturnUrlValidator {

    private final PaymentProperties paymentProperties;

    public void validate(URL returnUrl) {
        var origin = originOf(returnUrl);
        var allowed = paymentProperties
            .allowedReturnOrigins()
            .stream()
            .anyMatch(allowedOrigin -> origin.equals(originOf(allowedOrigin)));
        if (!allowed) {
            throw new PaymentInitializationFailedException(
                BusinessExceptionErrorCode.PAYMENT_RETURN_URL_NOT_ALLOWED,
                "returnUrl '%s' is not one of the configured allowed return origins.".formatted(returnUrl)
            );
        }
    }

    private URI originOf(URL url) {
        try {
            // Normalize an implicit port (-1) to the scheme's default port, so e.g. "https://host" and
            // "https://host:443" are recognized as the same origin.
            var port = url.getPort() == -1 ? url.getDefaultPort() : url.getPort();
            return new URI(url.getProtocol(), null, url.getHost(), port, null, null, null);
        } catch (URISyntaxException e) {
            throw new PaymentInitializationFailedException(
                BusinessExceptionErrorCode.PAYMENT_RETURN_URL_NOT_ALLOWED,
                "returnUrl '%s' is not a valid URL.".formatted(url)
            );
        }
    }

    private URI originOf(String url) {
        try {
            return originOf(URI.create(url).toURL());
        } catch (MalformedURLException | IllegalArgumentException e) {
            throw new IllegalStateException("Configured allowed return origin is not a valid URL: " + url, e);
        }
    }
}
