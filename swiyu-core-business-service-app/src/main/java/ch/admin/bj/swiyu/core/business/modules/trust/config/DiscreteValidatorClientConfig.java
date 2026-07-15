package ch.admin.bj.swiyu.core.business.modules.trust.config;

import ch.admin.bj.swiyu.discrete.validator.DefaultDiscreteValidatorClient;
import ch.admin.bj.swiyu.discrete.validator.DiscreteValidationResult;
import ch.admin.bj.swiyu.discrete.validator.DiscreteValidatorClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DiscreteValidatorClientConfig {

    private final TrustOnboardingSubmissionDoiValidationProperties properties;
    private final ObjectMapper objectMapper;

    @Bean
    @ConditionalOnExpression("'${app.limits.trust-onboarding-submission.doi-validation.mandant:}' != 'NONE'")
    public DiscreteValidatorClient discreteValidatorClient() {
        log.info("Creating SignatureValidatorClient since it is needed for current mandant configuration");
        return new DefaultDiscreteValidatorClient(
            objectMapper,
            properties.discreteValidatorService().url(),
            properties.discreteValidatorService().username(),
            properties.discreteValidatorService().password()
        );
    }

    @Bean
    @ConditionalOnProperty(
        prefix = "app.limits.trust-onboarding-submission.doi-validation",
        name = "mandant",
        havingValue = "NONE"
    )
    public DiscreteValidatorClient discreteValidatorClientMock() {
        log.warn("Creating mock SignatureValidatorClient since mandant for doi-validation is configured with NONE");
        return (_, _, expectedAmountOfSignatories) -> {
            log.warn(
                "Executing mock discrete validation request with expected amount of signatories: {}. Will return valid result.",
                expectedAmountOfSignatories
            );
            return new DiscreteValidationResult(
                true,
                JsonNodeFactory.instance.objectNode(),
                expectedAmountOfSignatories
            );
        };
    }
}
