package com.loomi.orderprocessing.dto;

import java.time.Instant;
import java.util.UUID;

public record OrderResultEvent(
        String eventId,
        String eventType,
        String timestamp,
        Payload payload
) {
    public record Payload(
            String orderId,
            String reason,
            String processedAt
    ) {}

    public static OrderResultEvent processed(String orderId) {
        return new OrderResultEvent(
                UUID.randomUUID().toString(),
                "ORDER_PROCESSED",
                Instant.now().toString(),
                new Payload(orderId, null, Instant.now().toString())
        );
    }

    public static OrderResultEvent failed(String orderId, String reason) {
        return new OrderResultEvent(
                UUID.randomUUID().toString(),
                "ORDER_FAILED",
                Instant.now().toString(),
                new Payload(orderId, reason, Instant.now().toString())
        );
    }

    public static OrderResultEvent pendingApproval(String orderId) {
        return new OrderResultEvent(
                UUID.randomUUID().toString(),
                "ORDER_PENDING_APPROVAL",
                Instant.now().toString(),
                new Payload(orderId, null, Instant.now().toString())
        );
    }
}
