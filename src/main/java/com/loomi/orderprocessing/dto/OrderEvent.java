package com.loomi.orderprocessing.dto;

import java.time.Instant;
import java.util.UUID;

public record OrderEvent(
        String eventId,
        String eventType,
        String timestamp,
        Payload payload
) {
    public record Payload(
            String orderId,
            String customerId
    ) {}

    public static OrderEvent orderCreated(String orderId, String customerId) {
        return new OrderEvent(
                UUID.randomUUID().toString(),
                "ORDER_CREATED",
                Instant.now().toString(),
                new Payload(orderId, customerId)
        );
    }

    public String orderId() {
        return payload.orderId();
    }
}
