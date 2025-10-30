package com.example.order.service;

import com.example.order.dto.*;
import com.example.order.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrderService {
    OrderResponse create(OrderCreateRequest request);
    OrderResponse getById(UUID id);
    Page<OrderResponse> list(OrderStatus status, Pageable pageable);
    Page<OrderResponse> getOrdersByCustomer(String customerId, Pageable pageable);
    OrderResponse updateStatus(UUID id, OrderStatus newStatus);
    OrderResponse cancel(UUID id);
}

