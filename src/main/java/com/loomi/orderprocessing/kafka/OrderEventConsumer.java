package com.loomi.orderprocessing.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loomi.orderprocessing.dto.OrderEvent;
import com.loomi.orderprocessing.model.enums.OrderStatus;
import com.loomi.orderprocessing.repository.OrderRepository;
import com.loomi.orderprocessing.service.OrderProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@Profile("!default")
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final OrderRepository orderRepository;
    private final OrderProcessingService orderProcessingService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order-events", groupId = "order-processing-group")
    public void consume(String payload) {
        try {
            OrderEvent event = objectMapper.readValue(payload, OrderEvent.class);
            processEvent(event);
        } catch (Exception e) {
            log.error("Failed to process event: {}", payload, e);
        }
    }

    private void processEvent(OrderEvent event) {
        var order = orderRepository.findById(event.orderId()).orElse(null);

        if (order == null) {
            log.warn("Order not found: {}", event.orderId());
            return;
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            log.info("Order {} already processed with status {}, skipping", event.orderId(), order.getStatus());
            return;
        }

        OrderStatus finalStatus = orderProcessingService.process(order);
        order.setStatus(finalStatus);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        log.info("Order {} processed with status {}", event.orderId(), finalStatus);
    }
}
