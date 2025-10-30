package com.example.order.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller for service discovery information.
 * Provides endpoints to view registered services and instances.
 */
@RestController
@RequestMapping("/api/discovery")
@Tag(name = "Service Discovery", description = "Endpoints for service discovery information")
public class ServiceDiscoveryController {

    private final DiscoveryClient discoveryClient;

    public ServiceDiscoveryController(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    @GetMapping("/services")
    @Operation(summary = "List all registered services",
            description = "Returns a list of all services registered with Eureka")
    public List<String> getServices() {
        return discoveryClient.getServices();
    }

    @GetMapping("/services/{serviceName}")
    @Operation(summary = "Get instances of a specific service",
            description = "Returns all instances of a specific service")
    public List<Map<String, Object>> getServiceInstances(@PathVariable String serviceName) {
        return discoveryClient.getInstances(serviceName).stream()
                .map(this::mapServiceInstance)
                .collect(Collectors.toList());
    }

    @GetMapping("/info")
    @Operation(summary = "Get current service information",
            description = "Returns information about the current service instance")
    public Map<String, Object> getServiceInfo() {
        List<ServiceInstance> instances = discoveryClient.getInstances("order");

        return Map.of(
                "serviceName", "order",
                "instanceCount", instances.size(),
                "instances", instances.stream()
                        .map(this::mapServiceInstance)
                        .collect(Collectors.toList()),
                "allServices", discoveryClient.getServices()
        );
    }

    private Map<String, Object> mapServiceInstance(ServiceInstance instance) {
        return Map.of(
                "instanceId", instance.getInstanceId(),
                "host", instance.getHost(),
                "port", instance.getPort(),
                "uri", instance.getUri().toString(),
                "metadata", instance.getMetadata(),
                "secure", instance.isSecure()
        );
    }
}

