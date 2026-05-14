package com.loomi.orderprocessing.service.processor;

import com.loomi.orderprocessing.model.Order;
import com.loomi.orderprocessing.model.OrderItem;
import com.loomi.orderprocessing.model.enums.ProductType;

public interface OrderProcessor {
    ProductType getSupportedType();
    ProcessingResult process(OrderItem item, String customerId, Order order);
}
