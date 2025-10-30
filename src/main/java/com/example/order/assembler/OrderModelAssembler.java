package com.example.order.assembler;

import com.example.order.controller.OrderController;
import com.example.order.dto.OrderResponse;
import com.example.order.dto.OrderStatusUpdateRequest;
import com.example.order.model.OrderStatus;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

/**
 * HATEOAS Model Assembler for Order entities following Richardson Maturity Model Level 3.
 * This assembler adds hypermedia links to OrderResponse objects, enabling clients to
 * discover available actions dynamically based on the order's current state.
 */
@Component
public class OrderModelAssembler implements RepresentationModelAssembler<OrderResponse, OrderResponse> {

    @NonNull
    @Override
    public OrderResponse toModel(@NonNull OrderResponse order) {
        // Self link - link to the order itself
        order.add(linkTo(methodOn(OrderController.class).get(order.getId())).withSelfRel());

        // Collection link - link to all orders
        order.add(linkTo(methodOn(OrderController.class).list(null, 0, 20)).withRel("orders"));

        // Customer orders link - link to all orders for this customer
        order.add(linkTo(methodOn(OrderController.class).getByCustomer(order.getCustomerId(), 0, 20))
                .withRel("customer-orders"));

        // State-based hypermedia controls - add links based on order status
        addStateBasedLinks(order);

        return order;
    }

    @NonNull
    @Override
    public CollectionModel<OrderResponse> toCollectionModel(@NonNull Iterable<? extends OrderResponse> entities) {
        CollectionModel<OrderResponse> orderModels = RepresentationModelAssembler.super.toCollectionModel(entities);

        // Add link to the collection itself
        orderModels.add(linkTo(methodOn(OrderController.class).list(null, 0, 20)).withSelfRel());

        return orderModels;
    }

    /**
     * Adds hypermedia links based on the current state of the order.
     * This implements true HATEOAS by exposing only the actions that are valid
     * for the order's current state.
     */
    private void addStateBasedLinks(OrderResponse order) {
        OrderStatus status = order.getStatus();

        switch (status) {
            case PENDING:
                // From PENDING, order can be processed or cancelled
                order.add(createUpdateStatusLink(order.getId(), OrderStatus.PROCESSING, "process"));
                order.add(createCancelLink(order.getId()));
                break;

            case PROCESSING:
                // From PROCESSING, order can be shipped or cancelled
                order.add(createUpdateStatusLink(order.getId(), OrderStatus.SHIPPED, "ship"));
                order.add(createCancelLink(order.getId()));
                break;

            case SHIPPED:
                // From SHIPPED, order can be delivered
                order.add(createUpdateStatusLink(order.getId(), OrderStatus.DELIVERED, "deliver"));
                break;

            case DELIVERED:
                // Terminal state - no further actions available
                // No additional links needed
                break;

            case CANCELLED:
                // Terminal state - no further actions available
                // No additional links needed
                break;
        }
    }

    /**
     * Creates a link for updating order status.
     */
    private Link createUpdateStatusLink(java.util.UUID orderId, OrderStatus targetStatus, String action) {
        OrderStatusUpdateRequest request = OrderStatusUpdateRequest.builder()
                .status(targetStatus)
                .build();
        return linkTo(methodOn(OrderController.class)
                .updateStatus(orderId, request))
                .withRel(action);
    }

    /**
     * Creates a link for cancelling an order.
     */
    private Link createCancelLink(java.util.UUID orderId) {
        return linkTo(methodOn(OrderController.class)
                .cancel(orderId))
                .withRel("cancel");
    }
}

