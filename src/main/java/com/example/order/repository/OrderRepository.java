package com.example.order.repository;

import com.example.order.model.Order;
import com.example.order.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    // Optimized query with entity graph to avoid N+1 problem
    @EntityGraph(attributePaths = {"items"})
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") UUID id);

    // Find order by idempotency key for duplicate prevention
    @EntityGraph(attributePaths = {"items"})
    Optional<Order> findByIdempotencyKey(String idempotencyKey);

    // Optimized pagination query
    @EntityGraph(attributePaths = {"items"})
    Page<Order> findAllByStatus(OrderStatus status, Pageable pageable);

    // Find orders by customer with items
    @EntityGraph(attributePaths = {"items"})
    @Query("select o from Order o where o.customerId = :customerId")
    Page<Order> findByCustomerId(@Param("customerId") String customerId, Pageable pageable);

    // Count orders by status for statistics (no entity graph needed)
    @Query("select count(o) from Order o where o.status = :status")
    long countByStatus(@Param("status") OrderStatus status);

    // Bulk update for scheduler
    @Modifying
    @Query("update Order o set o.status = :to where o.status = :from and o.canceledAt is null")
    int bulkUpdateStatus(@Param("from") OrderStatus from, @Param("to") OrderStatus to);
}

