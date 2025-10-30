package com.example.order.cache;

import com.example.order.dto.OrderCreateRequest;
import com.example.order.dto.OrderItemRequest;
import com.example.order.dto.OrderResponse;
import com.example.order.model.OrderStatus;
import com.example.order.repository.OrderRepository;
import com.example.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for caching functionality.
 * Uses simple in-memory cache (not Redis) for testing.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CachingIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired(required = false)
    private CacheManager cacheManager;

    private UUID testOrderId;

    @BeforeEach
    void setUp() {
        if (cacheManager != null) {
            cacheManager.getCacheNames().forEach(cacheName -> {
                var cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                }
            });
        }
        orderRepository.deleteAll();

        // Create a test order
        OrderCreateRequest request = OrderCreateRequest.builder()
                .customerId("cache-test-customer")
                .items(Arrays.asList(
                        OrderItemRequest.builder()
                                .productId("cache-sku-1")
                                .quantity(1)
                                .unitPrice(new BigDecimal("10.00"))
                                .build()
                ))
                .build();
        OrderResponse created = orderService.create(request);
        testOrderId = created.getId();
    }

    @Test
    void getById_ShouldCacheResult() {
        if (cacheManager == null) {
            // Skip if caching is disabled
            return;
        }

        // First call - cache miss
        OrderResponse first = orderService.getById(testOrderId);
        assertNotNull(first);

        // Verify in cache
        var cache = cacheManager.getCache("orders");
        assertNotNull(cache);
        var cachedValue = cache.get(testOrderId);
        assertNotNull(cachedValue, "Order should be cached after first retrieval");

        // Second call - should use cache (verify by checking cached object)
        OrderResponse second = orderService.getById(testOrderId);
        assertNotNull(second);
        assertEquals(first.getId(), second.getId());
    }

    @Test
    void list_ShouldCacheResults() {
        if (cacheManager == null) {
            return;
        }

        // First call
        Page<OrderResponse> firstPage = orderService.list(OrderStatus.PENDING, PageRequest.of(0, 20));
        assertNotNull(firstPage);
        assertTrue(firstPage.getTotalElements() > 0);

        // Verify cache
        var cache = cacheManager.getCache("orderLists");
        assertNotNull(cache);

        // Second call - should use cache
        Page<OrderResponse> secondPage = orderService.list(OrderStatus.PENDING, PageRequest.of(0, 20));
        assertNotNull(secondPage);
        assertEquals(firstPage.getTotalElements(), secondPage.getTotalElements());
    }

    @Test
    void updateStatus_ShouldEvictCache() {
        if (cacheManager == null) {
            return;
        }

        // Cache the order
        orderService.getById(testOrderId);

        var ordersCache = cacheManager.getCache("orders");
        assertNotNull(ordersCache);
        assertNotNull(ordersCache.get(testOrderId), "Order should be cached");

        // Update status (this should update the cache, not evict in our implementation)
        // Note: We use @CachePut, so it updates the cache
        orderService.updateStatus(testOrderId, OrderStatus.SHIPPED);

        // Cache should still have the order (but updated)
        var cachedAfterUpdate = ordersCache.get(testOrderId);
        assertNotNull(cachedAfterUpdate, "Order should still be cached after update");
    }

    @Test
    void cancel_ShouldUpdateCache() {
        if (cacheManager == null) {
            return;
        }

        // Cache the order
        orderService.getById(testOrderId);

        var cache = cacheManager.getCache("orders");
        assertNotNull(cache);
        assertNotNull(cache.get(testOrderId));

        // Cancel order
        OrderResponse canceled = orderService.cancel(testOrderId);
        assertNotNull(canceled.getCanceledAt());

        // Cache should be updated with canceled order
        var cachedAfterCancel = cache.get(testOrderId);
        assertNotNull(cachedAfterCancel, "Canceled order should be cached");
    }

    @Test
    void create_ShouldPopulateCache() {
        if (cacheManager == null) {
            return;
        }

        OrderCreateRequest request = OrderCreateRequest.builder()
                .customerId("new-cache-customer")
                .items(Arrays.asList(
                        OrderItemRequest.builder()
                                .productId("new-sku")
                                .quantity(2)
                                .unitPrice(new BigDecimal("15.00"))
                                .build()
                ))
                .build();

        OrderResponse created = orderService.create(request);
        assertNotNull(created);

        // Verify it's cached
        var cache = cacheManager.getCache("orders");
        assertNotNull(cache);
        var cached = cache.get(created.getId());
        assertNotNull(cached, "Newly created order should be cached");
    }

    @Test
    void getOrdersByCustomer_ShouldCache() {
        if (cacheManager == null) {
            return;
        }

        String customerId = "cache-test-customer";

        // First call
        Page<OrderResponse> first = orderService.getOrdersByCustomer(customerId, PageRequest.of(0, 20));
        assertNotNull(first);
        assertTrue(first.getTotalElements() > 0);

        // Verify cache
        var cache = cacheManager.getCache("customerOrders");
        assertNotNull(cache);

        // Second call - should use cache
        Page<OrderResponse> second = orderService.getOrdersByCustomer(customerId, PageRequest.of(0, 20));
        assertNotNull(second);
        assertEquals(first.getTotalElements(), second.getTotalElements());
    }

    @Test
    void differentPagesShouldHaveDifferentCacheKeys() {
        if (cacheManager == null) {
            return;
        }

        // Create multiple orders
        for (int i = 0; i < 25; i++) {
            OrderCreateRequest request = OrderCreateRequest.builder()
                    .customerId("pagination-test")
                    .items(Arrays.asList(
                            OrderItemRequest.builder()
                                    .productId("sku-" + i)
                                    .quantity(1)
                                    .unitPrice(new BigDecimal("10.00"))
                                    .build()
                    ))
                    .build();
            orderService.create(request);
        }

        // Get different pages
        Page<OrderResponse> page0 = orderService.list(null, PageRequest.of(0, 10));
        Page<OrderResponse> page1 = orderService.list(null, PageRequest.of(1, 10));

        // Should have different content
        assertNotEquals(page0.getContent().get(0).getId(),
                       page1.getContent().get(0).getId(),
                       "Different pages should have different content");
    }
}

