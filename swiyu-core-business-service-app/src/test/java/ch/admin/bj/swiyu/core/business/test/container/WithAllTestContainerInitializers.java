package ch.admin.bj.swiyu.core.business.test.container;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Testcontainers
@ContextConfiguration(initializers = { PostgresSqlContainerInitializer.class, S3ContainerInitializer.class })
public @interface WithAllTestContainerInitializers {}
