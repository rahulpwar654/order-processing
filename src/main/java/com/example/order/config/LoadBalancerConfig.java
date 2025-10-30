package com.example.order.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration for Spring Cloud LoadBalancer.
 * Enables client-side load balancing for inter-service communication.
 */
@Configuration
public class LoadBalancerConfig {

    /**
     * RestTemplate with load balancing capability.
     * Use this for synchronous HTTP calls to other services.
     *
     * Example:
     * restTemplate.getForObject("http://SERVICE-NAME/api/endpoint", ResponseType.class)
     */
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * WebClient.Builder with load balancing capability.
     * Use this for reactive/async HTTP calls to other services.
     *
     * Example:
     * webClientBuilder.build()
     *   .get()
     *   .uri("http://SERVICE-NAME/api/endpoint")
     *   .retrieve()
     *   .bodyToMono(ResponseType.class);
     */
    @Bean
    @LoadBalanced
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}

