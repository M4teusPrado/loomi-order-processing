package com.loomi.orderprocessing.dto;

import com.loomi.orderprocessing.model.Order;
import com.loomi.orderprocessing.model.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        String orderId,
        String customerId,
        OrderStatus status,
        String failureReason,
        BigDecimal totalAmount,
        List<OrderItemResponse> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getOrderId(),
                order.getCustomerId(),
                order.getStatus(),
                order.getFailureReason(),
                order.getTotalAmount(),
                order.getItems().stream().map(OrderItemResponse::from).toList(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
