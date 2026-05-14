package com.loomi.orderprocessing.fixture;

import com.loomi.orderprocessing.model.Order;
import com.loomi.orderprocessing.model.OrderItem;
import com.loomi.orderprocessing.model.enums.OrderStatus;
import com.loomi.orderprocessing.model.enums.ProductType;

import java.math.BigDecimal;
import java.util.List;

public class OrderMock {

    public static OrderItem buildItem(String productId, ProductType type) {
        var item = new OrderItem();
        item.setProductId(productId);
        item.setProductType(type);
        item.setQuantity(1);
        item.setUnitPrice(BigDecimal.TEN);
        return item;
    }

    public static OrderItem subscriptionItem(String productId) {
        return buildItem(productId, ProductType.SUBSCRIPTION);
    }

    public static Order buildProcessedOrderWith(String productId, ProductType type) {
        var order = new Order();
        order.setStatus(OrderStatus.PROCESSED);
        order.setItems(List.of(buildItem(productId, type)));
        return order;
    }

    public static Order processedSubscriptionOrder(String productId) {
        return buildProcessedOrderWith(productId, ProductType.SUBSCRIPTION);
    }
}
