package com.loomi.orderprocessing.dto;

import com.loomi.orderprocessing.model.OrderItem;
import com.loomi.orderprocessing.model.enums.ProductType;

import java.math.BigDecimal;
import java.util.Map;

public record OrderItemResponse(
        String productId,
        ProductType productType,
        int quantity,
        BigDecimal unitPrice,
        Map<String, Object> metadata
) {
    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getProductId(),
                item.getProductType(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getMetadata()
        );
    }
}
