package com.example.order.controller;

import com.example.order.dto.*;
import com.example.order.model.OrderStatus;
import com.example.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Operations related to orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new order",
            description = "Creates a new order with customer and item details. " +
                         "Supports idempotency via Idempotency-Key header or idempotencyKey field in request body. " +
                         "Duplicate requests with the same key will return the existing order.",
            parameters = {
                    @Parameter(name = "Idempotency-Key", description = "Optional unique key to prevent duplicate order creation",
                              required = false, in = ParameterIn.HEADER)
            },
            responses = {
                    @ApiResponse(responseCode = "201", description = "Order created",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = OrderResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content),
                    @ApiResponse(responseCode = "409", description = "Conflict - duplicate order", content = @Content)
            })
    public OrderResponse create(@Valid @RequestBody OrderCreateRequest request,
                                @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKeyHeader) {
        // Priority: Header > Request Body > Auto-generated
        if (idempotencyKeyHeader != null && !idempotencyKeyHeader.isBlank()) {
            request.setIdempotencyKey(idempotencyKeyHeader);
        }
        return orderService.create(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID",
            parameters = {
                    @Parameter(name = "id", description = "Order ID", required = true, in = ParameterIn.PATH)
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Order found",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = OrderResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Order not found", content = @Content)
            })
    public OrderResponse get(@PathVariable UUID id) {
        return orderService.getById(id);
    }

    @GetMapping
    @Operation(summary = "List orders",
            description = "Lists orders with optional status filtering and pagination.",
            parameters = {
                    @Parameter(name = "status", description = "Filter by order status", required = false, in = ParameterIn.QUERY,
                            schema = @Schema(implementation = OrderStatus.class)),
                    @Parameter(name = "page", description = "Page number (0-based)", required = false, in = ParameterIn.QUERY),
                    @Parameter(name = "size", description = "Page size", required = false, in = ParameterIn.QUERY)
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Orders page",
                            content = @Content(mediaType = "application/json"))
            })
    public Page<OrderResponse> list(@RequestParam(value = "status", required = false) OrderStatus status,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return orderService.list(status, pageable);
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "List orders by customer",
            parameters = {
                    @Parameter(name = "customerId", description = "Customer ID", required = true, in = ParameterIn.PATH),
                    @Parameter(name = "page", description = "Page number (0-based)", required = false, in = ParameterIn.QUERY),
                    @Parameter(name = "size", description = "Page size", required = false, in = ParameterIn.QUERY)
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Orders page by customer",
                            content = @Content(mediaType = "application/json"))
            })
    public Page<OrderResponse> getByCustomer(@PathVariable String customerId,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return orderService.getOrdersByCustomer(customerId, pageable);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update order status",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Order status updated",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = OrderResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid status", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Order not found", content = @Content)
            })
    public OrderResponse updateStatus(@PathVariable UUID id, @Valid @RequestBody OrderStatusUpdateRequest request) {
        return orderService.updateStatus(id, request.getStatus());
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel order",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Order cancelled",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = OrderResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Order not found", content = @Content)
            })
    public OrderResponse cancel(@PathVariable UUID id) {
        return orderService.cancel(id);
    }
}
