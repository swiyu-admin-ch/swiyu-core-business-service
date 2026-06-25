package ch.admin.bj.swiyu.registry.identifier;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan
@EnableConfigurationProperties(IdentifierRegistryProperties.class)
public class IdentifierRegistryConfig {}
