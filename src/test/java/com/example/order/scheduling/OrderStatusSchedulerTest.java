package com.example.order.scheduling;

import com.example.order.model.Order;
import com.example.order.model.OrderStatus;
import com.example.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderStatusSchedulerTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderStatusScheduler scheduler;

    @Test
    void promotePendingToProcessing_ShouldUpdateOrders() {
        // Arrange
        when(orderRepository.bulkUpdateStatus(OrderStatus.PENDING, OrderStatus.PROCESSING))
                .thenReturn(5);

        // Act
        scheduler.promotePendingToProcessing();

        // Assert
        verify(orderRepository, times(1))
                .bulkUpdateStatus(OrderStatus.PENDING, OrderStatus.PROCESSING);
    }

    @Test
    void promotePendingToProcessing_ShouldHandleZeroUpdates() {
        // Arrange
        when(orderRepository.bulkUpdateStatus(OrderStatus.PENDING, OrderStatus.PROCESSING))
                .thenReturn(0);

        // Act
        scheduler.promotePendingToProcessing();

        // Assert
        verify(orderRepository, times(1))
                .bulkUpdateStatus(OrderStatus.PENDING, OrderStatus.PROCESSING);
    }

    @Test
    void promotePendingToProcessing_ShouldHandleManyUpdates() {
        // Arrange
        when(orderRepository.bulkUpdateStatus(OrderStatus.PENDING, OrderStatus.PROCESSING))
                .thenReturn(1000);

        // Act
        scheduler.promotePendingToProcessing();

        // Assert
        verify(orderRepository, times(1))
                .bulkUpdateStatus(OrderStatus.PENDING, OrderStatus.PROCESSING);
    }
}

