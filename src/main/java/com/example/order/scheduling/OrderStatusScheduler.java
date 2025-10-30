package com.example.order.scheduling;

import com.example.order.model.OrderStatus;
import com.example.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OrderStatusScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrderStatusScheduler.class);

    private final OrderRepository orderRepository;

    public OrderStatusScheduler(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    @Scheduled(cron = "0 */5 * * * *")
    @CacheEvict(value = {"orders", "orderLists"}, allEntries = true)
    public void promotePendingToProcessing() {
        int updated = orderRepository.bulkUpdateStatus(OrderStatus.PENDING, OrderStatus.PROCESSING);
        if (updated > 0) {
            log.info("Scheduler updated {} orders from PENDING to PROCESSING", updated);
            log.info("Cache cleared after bulk status update");
        }
    }
}

