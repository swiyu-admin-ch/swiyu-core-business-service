package ch.admin.bj.swiyu.core.business.common.s3;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class S3ClientConfiguration {

    public final S3Client s3Client;

    @Bean
    public S3Presigner s3Presigner() {
        var builder = S3Presigner.builder()
            .serviceConfiguration(serviceConfiguration())
            .region(s3Client.serviceClientConfiguration().region())
            .credentialsProvider(s3Client.serviceClientConfiguration().credentialsProvider());

        // set endpoint override if necessary
        s3Client.serviceClientConfiguration().endpointOverride().ifPresent(builder::endpointOverride);

        return builder.build();
    }

    private S3Configuration serviceConfiguration() {
        return S3Configuration.builder().pathStyleAccessEnabled(true).build();
    }
}
