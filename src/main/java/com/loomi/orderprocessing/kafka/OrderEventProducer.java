package com.loomi.orderprocessing.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loomi.orderprocessing.dto.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!default")
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
}
