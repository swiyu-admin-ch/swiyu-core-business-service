package ch.admin.bj.swiyu.core.business.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * As the default builder does not get injected if we want a second builder we need to do it our self.
 */
@Configuration
@RequiredArgsConstructor
public class ObjectMapperConfig {

    /**
     * The default object builder.
     */
    @Bean
    org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer objectMapperCustomizer() {
        return builder -> {};
    }
}
