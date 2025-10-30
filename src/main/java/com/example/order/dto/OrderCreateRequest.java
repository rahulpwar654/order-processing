package com.example.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCreateRequest {
    @NotBlank
    private String customerId;

    @Valid
    @NotEmpty
    private List<OrderItemRequest> items;

    /**
     * Optional idempotency key to prevent duplicate order creation.
     * If not provided, a hash of the request content will be used.
     * Same idempotency key within 24 hours will return the existing order.
     */
    private String idempotencyKey;
}

