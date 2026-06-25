package ch.admin.bj.swiyu.registry.status;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan
@EnableConfigurationProperties(StatusRegistryProperties.class)
public class StatusRegistryConfig {}
