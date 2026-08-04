package com.showroom.orchestrator.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Optional development CORS.
 *
 * The React frontend normally talks to this service through a same-origin
 * proxy, so CORS is disabled by default. Set the property
 * {@code showroom.cors.allowed-origins} (env {@code SHOWROOM_CORS_ALLOWED_ORIGINS},
 * comma-separated, e.g. {@code http://localhost:5173}) to enable it for direct
 * browser calls during development. Uses servlet (MVC) CORS configuration.
 */
@Configuration
public class CorsConfig {

    @Bean
    @ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${showroom.cors.allowed-origins:}')")
    public WebMvcConfigurer showroomCorsConfigurer(
            @Value("${showroom.cors.allowed-origins}") String allowedOrigins,
            @Value("${showroom.cors.allowed-methods:GET,POST,OPTIONS}") String allowedMethods,
            @Value("${showroom.cors.allowed-headers:*}") String allowedHeaders,
            @Value("${showroom.cors.allow-credentials:true}") boolean allowCredentials) {

        List<String> origins = Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList();
        List<String> methods = Arrays.stream(allowedMethods.split(",")).map(String::trim).toList();
        List<String> headers = Arrays.stream(allowedHeaders.split(",")).map(String::trim).toList();

        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins(origins.toArray(String[]::new))
                        .allowedMethods(methods.toArray(String[]::new))
                        .allowedHeaders(headers.toArray(String[]::new))
                        .allowCredentials(allowCredentials)
                        .maxAge(3600);
            }
        };
    }
}
