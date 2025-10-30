package com.example.order.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

/**
 * Test configuration for Redis.
 * Uses embedded Redis for testing or connects to localhost.
 */
@TestConfiguration
public class TestRedisConfig {

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        // For tests, connect to localhost Redis or use embedded
        // In actual test, we'll use @DataRedisTest with embedded Redis
        return new LettuceConnectionFactory("localhost", 6379);
    }
}

