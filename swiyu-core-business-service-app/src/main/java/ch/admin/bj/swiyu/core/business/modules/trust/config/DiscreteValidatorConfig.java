package ch.admin.bj.swiyu.core.business.modules.trust.config;

import ch.admin.suis.client.core.service.IValidationServiceClient;
import ch.admin.suis.client.core.service.ValidationServiceClientBuilder;
import ch.admin.suis.client.core.service.to.FileRequest;
import ch.admin.suis.client.core.service.to.StreamRequest;
import ch.admin.suis.client.core.service.to.UserRequest;
import ch.admin.suis.validator.rest.to.Mandator;
import ch.admin.suis.validator.rest.to.response.ValidationResponse;
import de.intarsys.aaa.authenticate.impl.UserPasswordCredential;
import de.intarsys.tools.crypto.Secret;
import java.security.Security;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class DiscreteValidatorConfig {

    private static final String BC_PROVIDER_NAME = "BC";

    private final TrustOnboardingSubmissionDoiValidationProperties properties;

    @Bean
    @ConditionalOnExpression("'${app.limits.trust-onboarding-submission.doi-validation.mandant:}' != 'NONE'")
    public IValidationServiceClient validatorServiceClient() {
        ensureBouncyCastleProviderRegistered();
        return ValidationServiceClientBuilder.newBuilder()
            .serviceUrl(properties.discreteValidatorService().url())
            .credential(
                new UserPasswordCredential(
                    properties.discreteValidatorService().username(),
                    Secret.hide(properties.discreteValidatorService().password().toCharArray())
                )
            )
            .build();
    }

    private static void ensureBouncyCastleProviderRegistered() {
        if (Security.getProvider(BC_PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Bean
    @ConditionalOnProperty(
        prefix = "app.limits.trust-onboarding-submission.doi-validation",
        name = "mandant",
        havingValue = "NONE"
    )
    public IValidationServiceClient validationServiceMock() {
        return new IValidationServiceClient() {
            @Override
            public Mandator[] getAllMandators(Locale locale) {
                throw new UnsupportedOperationException(
                    "Not implemented with discrete-validator-service.mandant being NONE"
                );
            }

            @Override
            public List<ValidationResponse> validateMultipleRequests(UserRequest userRequest, boolean b) {
                throw new UnsupportedOperationException(
                    "Not implemented with discrete-validator-service.mandant being NONE"
                );
            }

            @Override
            public List<ValidationResponse> validateMultipleRequests(UserRequest userRequest, boolean b, boolean b1) {
                throw new UnsupportedOperationException(
                    "Not implemented with discrete-validator-service.mandant being NONE"
                );
            }

            @Override
            public ValidationResponse validateOneRequest(
                List<FileRequest> list,
                boolean b,
                String s,
                String s1,
                String s2,
                String s3
            ) {
                throw new UnsupportedOperationException(
                    "Not implemented with discrete-validator-service.mandant being NONE"
                );
            }

            @Override
            public ValidationResponse validateOneRequest(
                List<FileRequest> list,
                boolean b,
                String s,
                String s1,
                String s2,
                String s3,
                boolean b1
            ) {
                throw new UnsupportedOperationException(
                    "Not implemented with discrete-validator-service.mandant being NONE"
                );
            }

            @Override
            public ValidationResponse validateOneRequest(
                List<FileRequest> list,
                boolean b,
                String s,
                String s1,
                String s2,
                String s3,
                boolean b1,
                boolean b2
            ) {
                throw new UnsupportedOperationException(
                    "Not implemented with discrete-validator-service.mandant being NONE"
                );
            }

            @Override
            public ValidationResponse validateOneRequestS(
                List<StreamRequest> list,
                boolean b,
                String s,
                String s1,
                String s2,
                String s3
            ) {
                throw new UnsupportedOperationException(
                    "Not implemented with discrete-validator-service.mandant being NONE"
                );
            }

            @Override
            public ValidationResponse validateOneRequestS(
                List<StreamRequest> list,
                boolean b,
                String s,
                String s1,
                String s2,
                String s3,
                boolean b1
            ) {
                throw new UnsupportedOperationException(
                    "Not implemented with discrete-validator-service.mandant being NONE"
                );
            }

            @Override
            public ValidationResponse validateOneRequestS(
                List<StreamRequest> list,
                boolean b,
                String s,
                String s1,
                String s2,
                String s3,
                boolean b1,
                boolean b2
            ) {
                throw new UnsupportedOperationException(
                    "Not implemented with discrete-validator-service.mandant being NONE"
                );
            }

            @Override
            public ValidationResponse validateOneRequestZip(
                List<FileRequest> list,
                String s,
                String s1,
                String s2,
                String s3,
                boolean b,
                boolean b1
            ) {
                throw new UnsupportedOperationException(
                    "Not implemented with discrete-validator-service.mandant being NONE"
                );
            }

            @Override
            public ValidationResponse validateOneSignature(
                byte[] bytes,
                String s,
                boolean b,
                String s1,
                String s2,
                String s3,
                String s4,
                String s5,
                String s6
            ) {
                throw new UnsupportedOperationException(
                    "Not implemented with discrete-validator-service.mandant being NONE"
                );
            }

            @Override
            public ValidationResponse validateOneSignature(
                byte[] bytes,
                String s,
                boolean b,
                String s1,
                String s2,
                String s3,
                String s4,
                String s5,
                String s6,
                boolean b1
            ) {
                throw new UnsupportedOperationException(
                    "Not implemented with discrete-validator-service.mandant being NONE"
                );
            }

            @Override
            public ValidationResponse validatePkcs7(CMSSignedData cmsSignedData, byte[] bytes, String s) {
                throw new UnsupportedOperationException(
                    "Not implemented with discrete-validator-service.mandant being NONE"
                );
            }

            @Override
            public ValidationResponse validatePkcs7(CMSSignedData cmsSignedData, byte[] bytes, String s, boolean b) {
                throw new UnsupportedOperationException(
                    "Not implemented with discrete-validator-service.mandant being NONE"
                );
            }
        };
    }
}
