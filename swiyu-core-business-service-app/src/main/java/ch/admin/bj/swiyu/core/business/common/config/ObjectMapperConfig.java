package ch.admin.bj.swiyu.core.business.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

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
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        return builder.build();
    }
}
