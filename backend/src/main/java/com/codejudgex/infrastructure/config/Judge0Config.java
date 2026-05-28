package com.codejudgex.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Configures the RestTemplate used exclusively for Judge0 CE HTTP calls.
 * Named "judge0RestTemplate" to avoid collision with any other RestTemplate beans.
 */
@Configuration
public class Judge0Config {

    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS    = 30_000;

    @Bean
    RestTemplate judge0RestTemplate(@Value("${app.judge0.token:}") String token) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);

        RestTemplate restTemplate = new RestTemplate(factory);

        if (token != null && !token.isBlank()) {
            restTemplate.getInterceptors().add((request, body, execution) -> {
                request.getHeaders().set("X-Auth-Token", token);
                return execution.execute(request, body);
            });
        }

        return restTemplate;
    }
}
