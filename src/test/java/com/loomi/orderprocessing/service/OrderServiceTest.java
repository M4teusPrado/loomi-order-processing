package com.loomi.orderprocessing.service;

import com.loomi.orderprocessing.dto.CreateOrderRequest;
import com.loomi.orderprocessing.dto.OrderItemRequest;
import com.loomi.orderprocessing.exception.OrderNotFoundException;
import com.loomi.orderprocessing.exception.ProductNotAvailableException;
import com.loomi.orderprocessing.exception.ProductNotFoundException;
import com.loomi.orderprocessing.kafka.OrderEventProducer;
import com.loomi.orderprocessing.model.Order;
import com.loomi.orderprocessing.model.enums.OrderStatus;
import com.loomi.orderprocessing.model.enums.ProductType;
import com.loomi.orderprocessing.repository.OrderRepository;
import com.loomi.orderprocessing.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.loomi.orderprocessing.fixture.OrderMock.buildProcessedOrderWith;
import static com.loomi.orderprocessing.fixture.ProductMock.laptopWithStock;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private ProductRepository productRepository;
    @Mock private OrderEventProducer eventProducer;

    @InjectMocks private OrderService orderService;

    private static final String CUSTOMER_ID = "customer-001";
    private static final String PRODUCT_ID = "LAPTOP-PRO-2024";

    @Test
    void shouldCreateOrderWithPendingStatus() {
        // Arrange
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(laptopWithStock(10)));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Act
        var response = orderService.createOrder(new CreateOrderRequest(CUSTOMER_ID, List.of(new OrderItemRequest(PRODUCT_ID, 1))));

        // Assert
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.customerId()).isEqualTo(CUSTOMER_ID);
    }

    @Test
    void shouldPublishKafkaEventOnOrderCreation() {
        // Arrange
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(laptopWithStock(10)));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Act
        orderService.createOrder(new CreateOrderRequest(CUSTOMER_ID, List.of(new OrderItemRequest(PRODUCT_ID, 1))));

        // Assert
        verify(eventProducer).send(any());
    }

    @Test
    void shouldThrowWhenProductNotFound() {
        // Arrange
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> orderService.createOrder(
                new CreateOrderRequest(CUSTOMER_ID, List.of(new OrderItemRequest(PRODUCT_ID, 1)))))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void shouldThrowWhenProductIsInactive() {
        // Arrange
        var product = laptopWithStock(10);
        product.setActive(false);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

        // Act & Assert
        assertThatThrownBy(() -> orderService.createOrder(
                new CreateOrderRequest(CUSTOMER_ID, List.of(new OrderItemRequest(PRODUCT_ID, 1)))))
                .isInstanceOf(ProductNotAvailableException.class);
    }

    @Test
    void shouldThrowWhenOrderNotFound() {
        // Arrange
        when(orderRepository.findById("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> orderService.getOrder("nonexistent"))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void shouldReturnOrdersByCustomer() {
        // Arrange
        var order = buildProcessedOrderWith(PRODUCT_ID, ProductType.PHYSICAL);
        order.setOrderId("order-1");
        order.setCustomerId(CUSTOMER_ID);
        when(orderRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(List.of(order));

        // Act
        var result = orderService.getOrdersByCustomer(CUSTOMER_ID);

        // Assert
        assertThat(result).hasSize(1);
    }
}
