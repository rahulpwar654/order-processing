package com.example.order.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;

/**
 * Custom health indicator for Service Discovery.
 * Shows the status of service registration and discovered services.
 */
@Component
public class ServiceDiscoveryHealthIndicator implements HealthIndicator {

    private final DiscoveryClient discoveryClient;

    public ServiceDiscoveryHealthIndicator(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    @Override
    public Health health() {
        try {
            int servicesCount = discoveryClient.getServices().size();

            if (servicesCount > 0) {
                return Health.up()
                        .withDetail("services-discovered", servicesCount)
                        .withDetail("service-names", discoveryClient.getServices())
                        .withDetail("status", "Connected to Eureka Server")
                        .build();
            } else {
                return Health.up()
                        .withDetail("services-discovered", 0)
                        .withDetail("status", "Connected but no services registered")
                        .build();
            }
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("status", "Unable to connect to Eureka Server")
                    .build();
        }
    }
}

