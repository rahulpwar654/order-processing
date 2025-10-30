package com.example.order.controller;

import com.example.order.assembler.OrderModelAssembler;
import com.example.order.dto.*;
import com.example.order.model.OrderStatus;
import com.example.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for Order management following Richardson Maturity Model Level 3 (HATEOAS).
 * This controller provides hypermedia-driven REST APIs where responses include links to
 * related resources and available actions based on the current state.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderModelAssembler orderModelAssembler;
    private final PagedResourcesAssembler<OrderResponse> pagedResourcesAssembler;

    public OrderController(OrderService orderService,
                          OrderModelAssembler orderModelAssembler,
                          PagedResourcesAssembler<OrderResponse> pagedResourcesAssembler) {
        this.orderService = orderService;
        this.orderModelAssembler = orderModelAssembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    /**
     * Creates a new order.
     * Returns HTTP 201 (Created) with hypermedia links including self, orders collection,
     * and state-based action links.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@Valid @RequestBody OrderCreateRequest request) {
        OrderResponse response = orderService.create(request);
        return orderModelAssembler.toModel(response);
    }

    /**
     * Retrieves a single order by ID.
     * Returns the order with hypermedia links for navigation and available actions.
     */
    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable UUID id) {
        OrderResponse response = orderService.getById(id);
        return orderModelAssembler.toModel(response);
    }

    /**
     * Lists all orders with optional filtering by status.
     * Returns a paged collection with hypermedia links for pagination and navigation.
     */
    @GetMapping
    public PagedModel<OrderResponse> list(@RequestParam(value = "status", required = false) OrderStatus status,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<OrderResponse> orderPage = orderService.list(status, pageable);

        // Add pagination links with HATEOAS support
        return pagedResourcesAssembler.toModel(orderPage, orderModelAssembler);
    }

    /**
     * Retrieves all orders for a specific customer.
     * Returns a paged collection with hypermedia links for pagination and navigation.
     */
    @GetMapping("/customer/{customerId}")
    public PagedModel<OrderResponse> getByCustomer(@PathVariable String customerId,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<OrderResponse> orderPage = orderService.getOrdersByCustomer(customerId, pageable);

        // Add pagination links with HATEOAS support
        return pagedResourcesAssembler.toModel(orderPage, orderModelAssembler);
    }

    /**
     * Updates the status of an order.
     * Returns the updated order with hypermedia links reflecting the new state and available actions.
     */
    @PatchMapping("/{id}/status")
    public OrderResponse updateStatus(@PathVariable UUID id, @Valid @RequestBody OrderStatusUpdateRequest request) {
        OrderResponse response = orderService.updateStatus(id, request.getStatus());
        return orderModelAssembler.toModel(response);
    }

    /**
     * Cancels an order.
     * Returns the cancelled order with hypermedia links reflecting the terminal state.
     */
    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@PathVariable UUID id) {
        OrderResponse response = orderService.cancel(id);
        return orderModelAssembler.toModel(response);
    }
}

