package ch.admin.bj.swiyu.core.business;

import ch.admin.bj.swiyu.registry.identifier.IdentifierRegistryConfig;
import ch.admin.bj.swiyu.registry.status.StatusRegistryConfig;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Slf4j
@Import({ IdentifierRegistryConfig.class, StatusRegistryConfig.class })
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
@EnableTransactionManagement(order = Ordered.LOWEST_PRECEDENCE - 1) // for jeap-messaging-idempotence/outbox
@SpringBootApplication
@ConfigurationPropertiesScan
public class Application {

    public static void main(String[] args) {
        Environment env = SpringApplication.run(Application.class, args).getEnvironment();
        log.info(
            """

            ----------------------------------------------------------
            {} is running!
            SwaggerUI: http://localhost:{}/swagger-ui.html
            Profile(s): {}
            ----------------------------------------------------------""",
            env.getProperty("spring.application.name"),
            env.getProperty("server.port"),
            env.getActiveProfiles()
        );
    }
}
