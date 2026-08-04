package com.showroom.orchestrator.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Blocking {@link RestClient} for the downstream Vehicle Config service, with
 * explicit connect and read timeouts so a slow or dead downstream never hangs
 * a GraphQL request indefinitely.
 *
 * <p>This service deliberately uses the servlet (MVC) stack: the GraphQL
 * transport and the downstream HTTP client are both synchronous. Timeouts are
 * enforced at the socket level by {@link SimpleClientHttpRequestFactory}
 * (connect timeout + inactivity/read timeout), which replaces the reactive
 * {@code Mono.timeout} behaviour of the previous WebClient-based client.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient vehicleServiceRestClient(@Value("${vehicle-service.url}") String baseUrl,
                                               @Value("${vehicle-service.connect-timeout:2000}") long connectTimeoutMs,
                                               @Value("${vehicle-service.request-timeout:5000}") long requestTimeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(requestTimeoutMs));

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
