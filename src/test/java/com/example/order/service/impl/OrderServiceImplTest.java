package com.example.order.service.impl;

import com.example.order.dto.*;
import com.example.order.exception.ConflictException;
import com.example.order.exception.NotFoundException;
import com.example.order.model.Order;
import com.example.order.model.OrderItem;
import com.example.order.model.OrderStatus;
import com.example.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private UUID orderId;
    private Order mockOrder;
    private OrderCreateRequest createRequest;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();

        // Setup mock order
        mockOrder = Order.builder()
                .id(orderId)
                .customerId("cust-123")
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("31.00"))
                .items(new ArrayList<>())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        OrderItem item1 = OrderItem.builder()
                .id(UUID.randomUUID())
                .productId("sku-1")
                .quantity(2)
                .unitPrice(new BigDecimal("10.50"))
                .lineTotal(new BigDecimal("21.00"))
                .order(mockOrder)
                .build();

        OrderItem item2 = OrderItem.builder()
                .id(UUID.randomUUID())
                .productId("sku-2")
                .quantity(2)
                .unitPrice(new BigDecimal("5.00"))
                .lineTotal(new BigDecimal("10.00"))
                .order(mockOrder)
                .build();

        mockOrder.getItems().add(item1);
        mockOrder.getItems().add(item2);

        // Setup create request
        createRequest = OrderCreateRequest.builder()
                .customerId("cust-123")
                .items(Arrays.asList(
                        OrderItemRequest.builder()
                                .productId("sku-1")
                                .quantity(2)
                                .unitPrice(new BigDecimal("10.50"))
                                .build(),
                        OrderItemRequest.builder()
                                .productId("sku-2")
                                .quantity(2)
                                .unitPrice(new BigDecimal("5.00"))
                                .build()
                ))
                .build();
    }

    @Test
    void create_ShouldCreateOrderSuccessfully() {
        // Arrange
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);

        // Act
        OrderResponse response = orderService.create(createRequest);

        // Assert
        assertNotNull(response);
        assertEquals("cust-123", response.getCustomerId());
        assertEquals(OrderStatus.PENDING, response.getStatus());
        assertEquals(new BigDecimal("31.00"), response.getTotalAmount());
        assertEquals(2, response.getItems().size());

        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void create_ShouldCalculateTotalAmountCorrectly() {
        // Arrange
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(orderId);
            return order;
        });

        // Act
        OrderResponse response = orderService.create(createRequest);

        // Assert
        assertEquals(new BigDecimal("31.00"), response.getTotalAmount());
        assertEquals(new BigDecimal("21.00"), response.getItems().get(0).getLineTotal());
        assertEquals(new BigDecimal("10.00"), response.getItems().get(1).getLineTotal());
    }

    @Test
    void create_ShouldThrowConflictException_WhenItemsEmpty() {
        // Arrange
        OrderCreateRequest emptyRequest = OrderCreateRequest.builder()
                .customerId("cust-123")
                .items(Collections.emptyList())
                .build();

        // Act & Assert
        ConflictException exception = assertThrows(ConflictException.class, () -> {
            orderService.create(emptyRequest);
        });

        assertEquals("Order must contain at least one item", exception.getMessage());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void create_ShouldThrowConflictException_WhenItemsNull() {
        // Arrange
        OrderCreateRequest nullRequest = OrderCreateRequest.builder()
                .customerId("cust-123")
                .items(null)
                .build();

        // Act & Assert
        ConflictException exception = assertThrows(ConflictException.class, () -> {
            orderService.create(nullRequest);
        });

        assertEquals("Order must contain at least one item", exception.getMessage());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void getById_ShouldReturnOrder_WhenOrderExists() {
        // Arrange
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(mockOrder));

        // Act
        OrderResponse response = orderService.getById(orderId);

        // Assert
        assertNotNull(response);
        assertEquals(orderId, response.getId());
        assertEquals("cust-123", response.getCustomerId());
        assertEquals(OrderStatus.PENDING, response.getStatus());

        verify(orderRepository, times(1)).findById(orderId);
    }

    @Test
    void getById_ShouldThrowNotFoundException_WhenOrderDoesNotExist() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();
        when(orderRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            orderService.getById(nonExistentId);
        });

        assertTrue(exception.getMessage().contains(nonExistentId.toString()));
        verify(orderRepository, times(1)).findById(nonExistentId);
    }

    @Test
    void list_ShouldReturnAllOrders_WhenStatusIsNull() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> orderPage = new PageImpl<>(Collections.singletonList(mockOrder));
        when(orderRepository.findAll(pageable)).thenReturn(orderPage);

        // Act
        Page<OrderResponse> response = orderService.list(null, pageable);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        assertEquals(orderId, response.getContent().get(0).getId());

        verify(orderRepository, times(1)).findAll(pageable);
        verify(orderRepository, never()).findAllByStatus(any(), any());
    }

    @Test
    void list_ShouldReturnFilteredOrders_WhenStatusProvided() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> orderPage = new PageImpl<>(Collections.singletonList(mockOrder));
        when(orderRepository.findAllByStatus(OrderStatus.PENDING, pageable)).thenReturn(orderPage);

        // Act
        Page<OrderResponse> response = orderService.list(OrderStatus.PENDING, pageable);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        assertEquals(OrderStatus.PENDING, response.getContent().get(0).getStatus());

        verify(orderRepository, times(1)).findAllByStatus(OrderStatus.PENDING, pageable);
        verify(orderRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void updateStatus_ShouldUpdateFromProcessingToShipped() {
        // Arrange
        mockOrder.setStatus(OrderStatus.PROCESSING);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(mockOrder));

        // Act
        OrderResponse response = orderService.updateStatus(orderId, OrderStatus.SHIPPED);

        // Assert
        assertNotNull(response);
        assertEquals(OrderStatus.SHIPPED, response.getStatus());
        verify(orderRepository, times(1)).findById(orderId);
    }

    @Test
    void updateStatus_ShouldUpdateFromShippedToDelivered() {
        // Arrange
        mockOrder.setStatus(OrderStatus.SHIPPED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(mockOrder));

        // Act
        OrderResponse response = orderService.updateStatus(orderId, OrderStatus.DELIVERED);

        // Assert
        assertNotNull(response);
        assertEquals(OrderStatus.DELIVERED, response.getStatus());
        verify(orderRepository, times(1)).findById(orderId);
    }

    @Test
    void updateStatus_ShouldThrowConflictException_WhenOrderNotFound() {
        // Arrange
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NotFoundException.class, () -> {
            orderService.updateStatus(orderId, OrderStatus.SHIPPED);
        });
    }

    @Test
    void updateStatus_ShouldThrowConflictException_WhenOrderCanceled() {
        // Arrange
        mockOrder.setCanceledAt(Instant.now());
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(mockOrder));

        // Act & Assert
        ConflictException exception = assertThrows(ConflictException.class, () -> {
            orderService.updateStatus(orderId, OrderStatus.PROCESSING);
        });

        assertEquals("Order has been canceled", exception.getMessage());
    }

    @Test
    void updateStatus_ShouldThrowConflictException_WhenStatusIsPending() {
        // Arrange
        mockOrder.setStatus(OrderStatus.PENDING);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(mockOrder));

        // Act & Assert
        ConflictException exception = assertThrows(ConflictException.class, () -> {
            orderService.updateStatus(orderId, OrderStatus.PROCESSING);
        });

        assertEquals("Cannot manually update status from PENDING", exception.getMessage());
    }

    @Test
    void updateStatus_ShouldThrowConflictException_WhenStatusIsDelivered() {
        // Arrange
        mockOrder.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(mockOrder));

        // Act & Assert
        ConflictException exception = assertThrows(ConflictException.class, () -> {
            orderService.updateStatus(orderId, OrderStatus.SHIPPED);
        });

        assertEquals("Order already delivered", exception.getMessage());
    }

    @Test
    void updateStatus_ShouldThrowConflictException_WhenInvalidTransitionFromProcessing() {
        // Arrange
        mockOrder.setStatus(OrderStatus.PROCESSING);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(mockOrder));

        // Act & Assert
        ConflictException exception = assertThrows(ConflictException.class, () -> {
            orderService.updateStatus(orderId, OrderStatus.DELIVERED);
        });

        assertEquals("Only allowed transition from PROCESSING is to SHIPPED", exception.getMessage());
    }

    @Test
    void updateStatus_ShouldThrowConflictException_WhenInvalidTransitionFromShipped() {
        // Arrange
        mockOrder.setStatus(OrderStatus.SHIPPED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(mockOrder));

        // Act & Assert
        ConflictException exception = assertThrows(ConflictException.class, () -> {
            orderService.updateStatus(orderId, OrderStatus.PROCESSING);
        });

        assertEquals("Only allowed transition from SHIPPED is to DELIVERED", exception.getMessage());
    }

    @Test
    void cancel_ShouldCancelOrder_WhenStatusIsPending() {
        // Arrange
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(mockOrder));

        // Act
        OrderResponse response = orderService.cancel(orderId);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getCanceledAt());
        assertEquals(OrderStatus.PENDING, response.getStatus());
        verify(orderRepository, times(1)).findById(orderId);
    }

    @Test
    void cancel_ShouldThrowNotFoundException_WhenOrderDoesNotExist() {
        // Arrange
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NotFoundException.class, () -> {
            orderService.cancel(orderId);
        });
    }

    @Test
    void cancel_ShouldThrowConflictException_WhenAlreadyCanceled() {
        // Arrange
        mockOrder.setCanceledAt(Instant.now());
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(mockOrder));

        // Act & Assert
        ConflictException exception = assertThrows(ConflictException.class, () -> {
            orderService.cancel(orderId);
        });

        assertEquals("Order already canceled", exception.getMessage());
    }

    @Test
    void cancel_ShouldThrowConflictException_WhenStatusIsNotPending() {
        // Arrange
        mockOrder.setStatus(OrderStatus.PROCESSING);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(mockOrder));

        // Act & Assert
        ConflictException exception = assertThrows(ConflictException.class, () -> {
            orderService.cancel(orderId);
        });

        assertEquals("Can only cancel orders in PENDING status", exception.getMessage());
    }

    @Test
    void cancel_ShouldThrowConflictException_WhenStatusIsShipped() {
        // Arrange
        mockOrder.setStatus(OrderStatus.SHIPPED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(mockOrder));

        // Act & Assert
        ConflictException exception = assertThrows(ConflictException.class, () -> {
            orderService.cancel(orderId);
        });

        assertEquals("Can only cancel orders in PENDING status", exception.getMessage());
    }

    @Test
    void cancel_ShouldThrowConflictException_WhenStatusIsDelivered() {
        // Arrange
        mockOrder.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(mockOrder));

        // Act & Assert
        ConflictException exception = assertThrows(ConflictException.class, () -> {
            orderService.cancel(orderId);
        });

        assertEquals("Can only cancel orders in PENDING status", exception.getMessage());
    }
}

