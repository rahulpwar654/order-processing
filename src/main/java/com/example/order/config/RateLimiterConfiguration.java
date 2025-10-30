package com.example.order.config;

import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RateLimiterConfiguration {

    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        // Default rate limiter configuration
        RateLimiterConfig defaultConfig = RateLimiterConfig.custom()
                .limitForPeriod(100)  // 100 requests
                .limitRefreshPeriod(Duration.ofSeconds(1))  // per second
                .timeoutDuration(Duration.ofMillis(500))  // wait max 500ms for permission
                .build();

        // Strict rate limiter for write operations
        RateLimiterConfig strictConfig = RateLimiterConfig.custom()
                .limitForPeriod(20)  // 20 requests
                .limitRefreshPeriod(Duration.ofSeconds(1))  // per second
                .timeoutDuration(Duration.ofMillis(100))  // wait max 100ms
                .build();

        // Lenient rate limiter for read operations
        RateLimiterConfig lenientConfig = RateLimiterConfig.custom()
                .limitForPeriod(200)  // 200 requests
                .limitRefreshPeriod(Duration.ofSeconds(1))  // per second
                .timeoutDuration(Duration.ofMillis(500))
                .build();

        RateLimiterRegistry registry = RateLimiterRegistry.of(defaultConfig);

        // Register named rate limiters
        registry.rateLimiter("orderCreate", strictConfig);
        registry.rateLimiter("orderUpdate", strictConfig);
        registry.rateLimiter("orderQuery", lenientConfig);
        registry.rateLimiter("orderList", lenientConfig);

        return registry;
    }
}

