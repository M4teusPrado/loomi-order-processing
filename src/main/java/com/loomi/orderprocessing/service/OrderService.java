package com.loomi.orderprocessing.service;

import com.loomi.orderprocessing.dto.CreateOrderRequest;
import com.loomi.orderprocessing.dto.OrderEvent;
import com.loomi.orderprocessing.dto.OrderResponse;
import com.loomi.orderprocessing.dto.OrderSummaryResponse;
import com.loomi.orderprocessing.exception.OrderNotFoundException;
import com.loomi.orderprocessing.exception.ProductNotAvailableException;
import com.loomi.orderprocessing.exception.ProductNotFoundException;
import com.loomi.orderprocessing.kafka.OrderEventProducer;
import com.loomi.orderprocessing.model.Order;
import com.loomi.orderprocessing.model.OrderItem;
import com.loomi.orderprocessing.model.enums.OrderStatus;
import com.loomi.orderprocessing.repository.OrderRepository;
import com.loomi.orderprocessing.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderEventProducer eventProducer;

    public OrderService(OrderRepository orderRepository,
                        ProductRepository productRepository,
                        @Autowired(required = false) OrderEventProducer eventProducer) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.eventProducer = eventProducer;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        var order = new Order();
        order.setOrderId(UUID.randomUUID().toString());
        order.setCustomerId(request.customerId());
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        BigDecimal total = BigDecimal.ZERO;

        for (var itemRequest : request.items()) {
            var product = productRepository.findById(itemRequest.productId())
                    .orElseThrow(() -> new ProductNotFoundException(itemRequest.productId()));

            if (!product.isActive()) {
                throw new ProductNotAvailableException(itemRequest.productId());
            }

            var item = new OrderItem();
            item.setOrder(order);
            item.setProductId(product.getProductId());
            item.setProductType(product.getProductType());
            item.setQuantity(itemRequest.quantity());
            item.setUnitPrice(product.getPrice());

            order.getItems().add(item);
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(itemRequest.quantity())));
        }

        order.setTotalAmount(total);
        orderRepository.save(order);

        if (eventProducer != null) {
            eventProducer.send(OrderEvent.orderCreated(order.getOrderId(), order.getCustomerId()));
        }

        return OrderResponse.from(order);
    }

    public OrderResponse getOrder(String orderId) {
        return orderRepository.findById(orderId)
                .map(OrderResponse::from)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    public List<OrderSummaryResponse> getOrdersByCustomer(String customerId) {
        return orderRepository.findByCustomerId(customerId).stream()
                .map(OrderSummaryResponse::from)
                .toList();
    }
}
