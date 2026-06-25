package ch.admin.bj.swiyu.core.business.test.container;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.utility.DockerImageName;

@Slf4j
public class S3ContainerInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static MinIOContainer container;

    private static MinIOContainer getContainer() {
        if (container == null) {
            container = new MinIOContainer(
                DockerImageName.parse(
                    "repo.bit.admin.ch:8444/minio/minio:RELEASE.2025-09-07T16-13-09Z"
                ).asCompatibleSubstituteFor("minio/minio:RELEASE.2025-09-07T16-13-09Z")
            );
            container.start();
            log.info("S3 container started at: {}", container.getS3URL());
        }
        return container;
    }

    public void initialize(@NotNull ConfigurableApplicationContext configurableApplicationContext) {
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
            configurableApplicationContext,
            "jeap.s3.client.endpoint-url=%s".formatted(getContainer().getS3URL()),
            "jeap.s3.client.access-key=%s".formatted(getContainer().getUserName()),
            "jeap.s3.client.secret-key=%s".formatted(getContainer().getPassword()),
            "jeap.s3.client.tls=false"
        );
    }
}
