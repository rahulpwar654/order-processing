package com.example.order.service;

import com.example.order.dto.OrderResponse;
import com.example.order.model.Order;
import com.example.order.model.OrderItem;

import java.util.stream.Collectors;

public class OrderMapper {
    public static OrderResponse toResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .customerId(order.getCustomerId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .items(order.getItems().stream().map(OrderMapper::toItem).collect(Collectors.toList()))
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .canceledAt(order.getCanceledAt())
                .build();
    }

    private static OrderResponse.Item toItem(OrderItem i) {
        return OrderResponse.Item.builder()
                .productId(i.getProductId())
                .quantity(i.getQuantity())
                .unitPrice(i.getUnitPrice())
                .lineTotal(i.getLineTotal())
                .build();
    }
}

