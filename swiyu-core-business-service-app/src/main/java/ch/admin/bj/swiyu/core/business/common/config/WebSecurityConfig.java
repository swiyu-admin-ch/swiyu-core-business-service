package ch.admin.bj.swiyu.core.business.common.config;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {

    private final CorsProperties corsProperties;

    /**
     * Public SecurityFilterChain for Saferpay B2B payment notification callback.
     * <p>
     * This endpoint is intentionally left unsecured (no authentication required).
     * It is designed to be called exclusively via the API Gateway without authentication.
     * The API Gateway ensures that only Saferpay IP addresses can invoke this endpoint.
     * </p>
     * <p>
     * Higher precedence (lower order) than the default jEAP security filter chain to ensure
     * this path is matched first and permitted without authentication.
     * </p>
     */
    @Bean
    @Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
    SecurityFilterChain saferpayNotifyFilterChain(HttpSecurity http) {
        http
            .securityMatcher("/api/v1/saferpay/notify")
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .csrf(csrf -> csrf.disable()); // NOSONAR: Saferpay webhook endpoint - external caller (no session/CSRF token), protected by API Gateway IP allowlist
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.getAllowedOrigins());
        configuration.setAllowedMethods(List.of("*"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
