package com.loomi.orderprocessing.dto;

import com.loomi.orderprocessing.model.Order;
import com.loomi.orderprocessing.model.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderSummaryResponse(
        String orderId,
        String customerId,
        OrderStatus status,
        BigDecimal totalAmount,
        LocalDateTime createdAt
) {
    public static OrderSummaryResponse from(Order order) {
        return new OrderSummaryResponse(
                order.getOrderId(),
                order.getCustomerId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCreatedAt()
        );
    }
}
