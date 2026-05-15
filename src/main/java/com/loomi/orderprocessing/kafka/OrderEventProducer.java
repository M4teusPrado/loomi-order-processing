package com.loomi.orderprocessing.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loomi.orderprocessing.dto.OrderEvent;
import com.loomi.orderprocessing.dto.OrderResultEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventProducer {

    private static final String TOPIC = "order-events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void send(OrderEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC, event.orderId(), payload);
            log.info("Event published for order {}", event.orderId());
        } catch (Exception e) {
            log.error("Failed to publish event for order {}", event.orderId(), e);
        }
    }

    public void sendResult(OrderResultEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("order-results", event.payload().orderId(), payload);
            log.info("Result event {} published for order {}", event.eventType(), event.payload().orderId());
        } catch (Exception e) {
            log.error("Failed to publish result event for order {}", event.payload().orderId(), e);
        }
    }
}
