package com.loomi.orderprocessing.dto;

public record OrderEvent(
        String orderId,
        String customerId
) {}
