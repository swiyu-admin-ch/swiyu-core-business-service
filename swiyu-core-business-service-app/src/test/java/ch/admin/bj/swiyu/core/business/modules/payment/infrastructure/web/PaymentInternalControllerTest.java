package ch.admin.bj.swiyu.core.business.modules.payment.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import ch.admin.bj.swiyu.core.business.common.api.ProductTypeDto;
import ch.admin.bj.swiyu.core.business.common.features.FeaturesProperties;
import ch.admin.bj.swiyu.core.business.common.security.AuthSupport;
import ch.admin.bj.swiyu.core.business.modules.payment.api.PaymentFilterDto;
import ch.admin.bj.swiyu.core.business.modules.payment.api.PaymentInitializationRequestDto;
import ch.admin.bj.swiyu.core.business.modules.payment.exceptions.PaymentDisabledException;
import ch.admin.bj.swiyu.core.business.modules.payment.service.PaymentService;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

class PaymentInternalControllerTest {

    private final PaymentService paymentService = mock(PaymentService.class);
    private final PaymentInternalController controller;

    PaymentInternalControllerTest() {
        var features = new FeaturesProperties();
        features.setEidartfe1297Payment(false);
        controller = new PaymentInternalController(paymentService, mock(AuthSupport.class), features);
    }

    @Test
    void featureDisabled_allEndpointsRefuseAndNeverCallService() throws MalformedURLException {
        var filter = new PaymentFilterDto(null, null, null);
        var pageable = Pageable.unpaged();
        var paymentId = UUID.randomUUID();
        var request = new PaymentInitializationRequestDto(
            UUID.randomUUID(),
            ProductTypeDto.BASE_REGISTRATION,
            URI.create("https://portal.example.local/callback").toURL()
        );

        assertThatThrownBy(() -> controller.getPayments(filter, pageable)).isInstanceOf(PaymentDisabledException.class);
        assertThatThrownBy(() -> controller.getPayment(paymentId)).isInstanceOf(PaymentDisabledException.class);
        assertThatThrownBy(() -> controller.initiatePayment(request)).isInstanceOf(PaymentDisabledException.class);
        verifyNoInteractions(paymentService);
    }
}
