package com.loomi.orderprocessing.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loomi.orderprocessing.dto.OrderEvent;
import com.loomi.orderprocessing.model.Order;
import com.loomi.orderprocessing.model.enums.OrderStatus;
import com.loomi.orderprocessing.repository.OrderRepository;
import com.loomi.orderprocessing.service.OrderProcessingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderEventConsumerTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderProcessingService orderProcessingService;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks private OrderEventConsumer consumer;

    private static final String ORDER_ID = "order-123";
    private static final String CUSTOMER_ID = "customer-123";

    private Order buildOrder(OrderStatus status) {
        var order = new Order();
        order.setOrderId(ORDER_ID);
        order.setCustomerId(CUSTOMER_ID);
        order.setStatus(status);
        return order;
    }

    @Test
    void shouldSkipProcessingWhenOrderIsAlreadyProcessed() throws Exception {
        // Arrange
        var event = OrderEvent.orderCreated(ORDER_ID, CUSTOMER_ID);
        var payload = new ObjectMapper().writeValueAsString(event);

        when(objectMapper.readValue(payload, OrderEvent.class)).thenReturn(event);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(buildOrder(OrderStatus.PROCESSED)));

        // Act
        consumer.consume(payload);

        // Assert - processing service should never be called
        verify(orderProcessingService, never()).process(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void shouldSkipProcessingWhenOrderIsAlreadyFailed() throws Exception {
        // Arrange
        var event = OrderEvent.orderCreated(ORDER_ID, CUSTOMER_ID);
        var payload = new ObjectMapper().writeValueAsString(event);

        when(objectMapper.readValue(payload, OrderEvent.class)).thenReturn(event);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(buildOrder(OrderStatus.FAILED)));

        // Act
        consumer.consume(payload);

        // Assert
        verify(orderProcessingService, never()).process(any());
    }

    @Test
    void shouldProcessWhenOrderIsPending() throws Exception {
        // Arrange
        var event = OrderEvent.orderCreated(ORDER_ID, CUSTOMER_ID);
        var payload = new ObjectMapper().writeValueAsString(event);
        var order = buildOrder(OrderStatus.PENDING);

        when(objectMapper.readValue(payload, OrderEvent.class)).thenReturn(event);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderProcessingService.process(order)).thenReturn(OrderStatus.PROCESSED);

        // Act
        consumer.consume(payload);

        // Assert
        verify(orderProcessingService).process(order);
        verify(orderRepository).save(order);
    }

    @Test
    void shouldSkipWhenOrderNotFound() throws Exception {
        // Arrange
        var event = OrderEvent.orderCreated(ORDER_ID, CUSTOMER_ID);
        var payload = new ObjectMapper().writeValueAsString(event);

        when(objectMapper.readValue(payload, OrderEvent.class)).thenReturn(event);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

        // Act
        consumer.consume(payload);

        // Assert
        verify(orderProcessingService, never()).process(any());
    }
}
