package com.example.order.dto;

import com.example.order.model.OrderStatus;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private UUID id;
    private String customerId;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private List<OrderResponse.Item> items;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant canceledAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Item implements Serializable {
        private static final long serialVersionUID = 1L;

        private String productId;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;
    }
}

