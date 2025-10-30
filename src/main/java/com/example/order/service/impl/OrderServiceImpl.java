package com.example.order.service.impl;

import com.example.order.dto.*;
import com.example.order.exception.ConflictException;
import com.example.order.exception.NotFoundException;
import com.example.order.exception.ServiceUnavailableException;
import com.example.order.model.Order;
import com.example.order.model.OrderItem;
import com.example.order.model.OrderStatus;
import com.example.order.repository.OrderRepository;
import com.example.order.service.OrderMapper;
import com.example.order.service.OrderService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.micrometer.observation.annotation.Observed;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderRepository orderRepository;
    private final Tracer tracer;

    public OrderServiceImpl(OrderRepository orderRepository, Tracer tracer) {
        this.orderRepository = orderRepository;
        this.tracer = tracer;
    }


    @Override
    @Caching(
        put = @CachePut(value = "orders", key = "#result.id"),
        evict = @CacheEvict(value = "orderLists", allEntries = true)
    )
    @CircuitBreaker(name = "orderService", fallbackMethod = "createFallback")
    @RateLimiter(name = "orderCreate")
    @Observed(name = "order.create", contextualName = "creating-order")
    public OrderResponse create(OrderCreateRequest request) {
        log.info("Creating order for customer: {}", request.getCustomerId());

        if (request.getItems() == null || request.getItems().isEmpty()) {
            log.error("Order creation failed: No items provided");
            throw new ConflictException("Order must contain at least one item");
        }

        // Generate or use provided idempotency key
        String idempotencyKey = request.getIdempotencyKey();
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            idempotencyKey = com.example.order.util.IdempotencyKeyGenerator.generateKey(request);
            log.debug("Generated idempotency key: {}", idempotencyKey);
        } else {
            log.debug("Using provided idempotency key: {}", idempotencyKey);
        }

        // Check for existing order with same idempotency key
        if (idempotencyKey != null) {
            Optional<Order> existingOrder = orderRepository.findByIdempotencyKey(idempotencyKey);
            if (existingOrder.isPresent()) {
                log.info("Order already exists with idempotency key: {}, returning existing order: {}",
                         idempotencyKey, existingOrder.get().getId());
                return OrderMapper.toResponse(existingOrder.get());
            }
        }

        Order order = Order.builder()
                .customerId(request.getCustomerId())
                .status(OrderStatus.PENDING)
                .idempotencyKey(idempotencyKey)
                .build();

        BigDecimal total = BigDecimal.ZERO;
        for (OrderItemRequest ir : request.getItems()) {
            BigDecimal lineTotal = ir.getUnitPrice().multiply(BigDecimal.valueOf(ir.getQuantity()));
            OrderItem item = OrderItem.builder()
                    .productId(ir.getProductId())
                    .quantity(ir.getQuantity())
                    .unitPrice(ir.getUnitPrice())
                    .lineTotal(lineTotal)
                    .build();
            order.addItem(item);
            total = total.add(lineTotal);
        }
        order.setTotalAmount(total);

        Order saved = orderRepository.save(order);

        // Add custom tags to current span
        if (tracer.currentSpan() != null) {
            tracer.currentSpan().tag("order.id", saved.getId().toString());
            tracer.currentSpan().tag("order.customerId", saved.getCustomerId());
            tracer.currentSpan().tag("order.itemCount", String.valueOf(saved.getItems().size()));
            tracer.currentSpan().tag("order.totalAmount", saved.getTotalAmount().toString());
        }

        log.info("Order created successfully: {} for customer: {}", saved.getId(), saved.getCustomerId());
        return OrderMapper.toResponse(saved);
    }

    @Override
    @Cacheable(value = "orders", key = "#id")
    @Transactional(readOnly = true)
    @CircuitBreaker(name = "orderService", fallbackMethod = "getByIdFallback")
    @RateLimiter(name = "orderQuery")
    @Observed(name = "order.getById", contextualName = "get-order-by-id")
    public OrderResponse getById(UUID id) {
        log.debug("Fetching order by ID: {}", id);

        // Use optimized query with entity graph to avoid N+1 problem
        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> {
                    log.warn("Order not found: {}", id);
                    return new NotFoundException("Order %s not found".formatted(id));
                });

        if (tracer.currentSpan() != null) {
            tracer.currentSpan().tag("order.id", id.toString());
            tracer.currentSpan().tag("order.status", order.getStatus().toString());
        }

        log.debug("Order retrieved successfully: {}", id);
        return OrderMapper.toResponse(order);
    }

    @Override
    @Cacheable(
        value = "orderLists",
        key = "T(String).format('%s:%d:%d', #status != null ? #status.name() : 'ALL', #pageable.pageNumber, #pageable.pageSize)"
    )
    @Transactional(readOnly = true)
    @CircuitBreaker(name = "orderService", fallbackMethod = "listFallback")
    @RateLimiter(name = "orderList")
    @Observed(name = "order.list", contextualName = "list-orders")
    public Page<OrderResponse> list(OrderStatus status, Pageable pageable) {
        log.debug("Listing orders - status: {}, page: {}, size: {}",
                 status, pageable.getPageNumber(), pageable.getPageSize());

        // Optimized queries with entity graph already defined in repository
        Page<Order> page = (status == null)
                ? orderRepository.findAll(pageable)
                : orderRepository.findAllByStatus(status, pageable);

        if (tracer.currentSpan() != null) {
            tracer.currentSpan().tag("order.status", status != null ? status.toString() : "ALL");
            tracer.currentSpan().tag("page.number", String.valueOf(pageable.getPageNumber()));
            tracer.currentSpan().tag("page.size", String.valueOf(pageable.getPageSize()));
            tracer.currentSpan().tag("result.totalElements", String.valueOf(page.getTotalElements()));
        }

        log.debug("Orders listed - total: {}, returned: {}", page.getTotalElements(), page.getNumberOfElements());
        return page.map(OrderMapper::toResponse);
    }

    @Override
    @Caching(
        put = @CachePut(value = "orders", key = "#id"),
        evict = @CacheEvict(value = "orderLists", allEntries = true)
    )
    @CircuitBreaker(name = "orderService", fallbackMethod = "updateStatusFallback")
    @RateLimiter(name = "orderUpdate")
    @Observed(name = "order.updateStatus", contextualName = "update-order-status")
    public OrderResponse updateStatus(UUID id, OrderStatus newStatus) {
        log.info("Updating order status: {} to {}", id, newStatus);

        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> {
                    log.warn("Order not found for status update: {}", id);
                    return new NotFoundException("Order %s not found".formatted(id));
                });
        if (order.getCanceledAt() != null) {
            throw new ConflictException("Order has been canceled");
        }
        if (order.getStatus() == OrderStatus.PENDING) {
            throw new ConflictException("Cannot manually update status from PENDING");
        }
        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new ConflictException("Order already delivered");
        }
        if (order.getStatus() == OrderStatus.PROCESSING && newStatus != OrderStatus.SHIPPED) {
            throw new ConflictException("Only allowed transition from PROCESSING is to SHIPPED");
        }
        if (order.getStatus() == OrderStatus.SHIPPED && newStatus != OrderStatus.DELIVERED) {
            throw new ConflictException("Only allowed transition from SHIPPED is to DELIVERED");
        }
        OrderStatus oldStatus = order.getStatus();
        order.setStatus(newStatus);
        order.setUpdatedAt(Instant.now());

        if (tracer.currentSpan() != null) {
            tracer.currentSpan().tag("order.id", id.toString());
            tracer.currentSpan().tag("order.oldStatus", oldStatus.toString());
            tracer.currentSpan().tag("order.newStatus", newStatus.toString());
        }

        log.info("Order status updated successfully: {} from {} to {}", id, oldStatus, newStatus);
        return OrderMapper.toResponse(order);
    }

    @Override
    @Cacheable(
        value = "customerOrders",
        key = "T(String).format('%s:%d:%d', #customerId, #pageable.pageNumber, #pageable.pageSize)"
    )
    @Transactional(readOnly = true)
    @CircuitBreaker(name = "orderService", fallbackMethod = "getOrdersByCustomerFallback")
    @RateLimiter(name = "orderQuery")
    @Observed(name = "order.getByCustomer", contextualName = "get-customer-orders")
    public Page<OrderResponse> getOrdersByCustomer(String customerId, Pageable pageable) {
        log.debug("Fetching orders for customer: {}", customerId);

        Page<Order> page = orderRepository.findByCustomerId(customerId, pageable);

        if (tracer.currentSpan() != null) {
            tracer.currentSpan().tag("order.customerId", customerId);
            tracer.currentSpan().tag("result.totalElements", String.valueOf(page.getTotalElements()));
        }

        log.debug("Customer orders retrieved - customer: {}, count: {}", customerId, page.getTotalElements());
        return page.map(OrderMapper::toResponse);
    }

    @Override
    @Caching(
        put = @CachePut(value = "orders", key = "#id"),
        evict = @CacheEvict(value = "orderLists", allEntries = true)
    )
    @CircuitBreaker(name = "orderService", fallbackMethod = "cancelFallback")
    @RateLimiter(name = "orderUpdate")
    @Observed(name = "order.cancel", contextualName = "cancel-order")
    public OrderResponse cancel(UUID id) {
        log.info("Cancelling order: {}", id);

        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> {
                    log.warn("Order not found for cancellation: {}", id);
                    return new NotFoundException("Order %s not found".formatted(id));
                });
        if (order.getCanceledAt() != null) {
            throw new ConflictException("Order already canceled");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ConflictException("Can only cancel orders in PENDING status");
        }
        order.setCanceledAt(Instant.now());
        order.setUpdatedAt(Instant.now());

        if (tracer.currentSpan() != null) {
            tracer.currentSpan().tag("order.id", id.toString());
            tracer.currentSpan().tag("order.status", order.getStatus().toString());
        }

        log.info("Order cancelled successfully: {}", id);
        return OrderMapper.toResponse(order);
    }

    // ========== Fallback Methods for Circuit Breaker ==========

    /**
     * Fallback method for create operation.
     * Returns a service unavailable error when circuit is open.
     */
    private OrderResponse createFallback(OrderCreateRequest request, Exception ex) {
        log.error("Circuit breaker triggered for order creation: {}", ex.getMessage());
        throw new ServiceUnavailableException("Order service is temporarily unavailable. Please try again later.");
    }

    /**
     * Fallback method for getById operation.
     * Returns a service unavailable error when circuit is open.
     */
    private OrderResponse getByIdFallback(UUID id, Exception ex) {
        log.error("Circuit breaker triggered for getById({}): {}", id, ex.getMessage());
        throw new ServiceUnavailableException("Order service is temporarily unavailable. Please try again later.");
    }

    /**
     * Fallback method for list operation.
     * Returns an empty page when circuit is open.
     */
    private Page<OrderResponse> listFallback(OrderStatus status, Pageable pageable, Exception ex) {
        throw new ServiceUnavailableException("Order listing service is temporarily unavailable. Please try again later.");
    }

    /**
     * Fallback method for updateStatus operation.
     * Returns a service unavailable error when circuit is open.
     */
    private OrderResponse updateStatusFallback(UUID id, OrderStatus newStatus, Exception ex) {
        throw new ServiceUnavailableException("Order update service is temporarily unavailable. Please try again later.");
    }

    /**
     * Fallback method for getOrdersByCustomer operation.
     * Returns an empty page when circuit is open.
     */
    private Page<OrderResponse> getOrdersByCustomerFallback(String customerId, Pageable pageable, Exception ex) {
        throw new ServiceUnavailableException("Customer order service is temporarily unavailable. Please try again later.");
    }

    /**
     * Fallback method for cancel operation.
     * Returns a service unavailable error when circuit is open.
     */
    private OrderResponse cancelFallback(UUID id, Exception ex) {
        throw new ServiceUnavailableException("Order cancellation service is temporarily unavailable. Please try again later.");
    }
}
