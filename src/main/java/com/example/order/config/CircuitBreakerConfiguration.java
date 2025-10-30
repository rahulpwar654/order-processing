package com.example.order.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class CircuitBreakerConfiguration {

    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> defaultCustomizer() {
        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(4))
                        .build())
                .circuitBreakerConfig(CircuitBreakerConfig.custom()
                        // Circuit breaker opens after 50% failure rate
                        .failureRateThreshold(50)
                        // Minimum number of calls before calculating failure rate
                        .minimumNumberOfCalls(5)
                        // Wait 60 seconds before transitioning from OPEN to HALF_OPEN
                        .waitDurationInOpenState(Duration.ofSeconds(60))
                        // Number of calls in HALF_OPEN state before re-evaluating
                        .permittedNumberOfCallsInHalfOpenState(3)
                        // Sliding window size for failure rate calculation
                        .slidingWindowSize(10)
                        .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                        // Slow call threshold
                        .slowCallRateThreshold(50)
                        .slowCallDurationThreshold(Duration.ofSeconds(3))
                        .build())
                .build());
    }
}

