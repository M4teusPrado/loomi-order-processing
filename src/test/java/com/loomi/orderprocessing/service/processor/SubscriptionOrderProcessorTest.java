package com.loomi.orderprocessing.service.processor;

import com.loomi.orderprocessing.model.Order;
import com.loomi.orderprocessing.model.enums.FailureReason;
import com.loomi.orderprocessing.repository.OrderRepository;
import com.loomi.orderprocessing.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.loomi.orderprocessing.fixture.OrderMock.*;
import static com.loomi.orderprocessing.fixture.ProductMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionOrderProcessorTest {

    @Mock private OrderRepository orderRepository;
    @Mock private ProductRepository productRepository;
    @InjectMocks private SubscriptionOrderProcessor processor;

    private static final String CUSTOMER_ID = "customer-123";
    private static final String PRODUCT_ID = "SUB-PREMIUM-001";

    private ProcessingResult process(String productId) {
        return processor.process(subscriptionItem(productId), CUSTOMER_ID, new Order());
    }

    @Test
    void shouldReturnSuccessWhenNoActiveSubscriptions() {
        // Arrange
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(premiumSubscription()));
        when(orderRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(List.of());

        // Act
        var result = process(PRODUCT_ID);

        // Assert
        assertThat(result.success()).isTrue();
        assertThat(result.metadata()).containsKey("subscriptionStart");
    }

    @Test
    void shouldFailWhenDuplicateActiveSubscription() {
        // Arrange
        when(orderRepository.findByCustomerId(CUSTOMER_ID))
                .thenReturn(List.of(processedSubscriptionOrder(PRODUCT_ID)));

        // Act & Assert
        assertThat(process(PRODUCT_ID).failureReason()).isEqualTo(FailureReason.DUPLICATE_ACTIVE_SUBSCRIPTION);
    }

    @Test
    void shouldFailWhenSubscriptionLimitExceeded() {
        // Arrange
        when(orderRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(List.of(
                processedSubscriptionOrder("SUB-A"),
                processedSubscriptionOrder("SUB-B"),
                processedSubscriptionOrder("SUB-C"),
                processedSubscriptionOrder("SUB-D"),
                processedSubscriptionOrder("SUB-E")
        ));

        // Act & Assert
        assertThat(process(PRODUCT_ID).failureReason()).isEqualTo(FailureReason.SUBSCRIPTION_LIMIT_EXCEEDED);
    }

    @Test
    void shouldFailWhenEnterpriseAndBasicAreIncompatible() {
        // Arrange
        when(productRepository.findById("SUB-BASIC-001")).thenReturn(Optional.of(basicSubscription()));
        when(productRepository.findById("SUB-ENTERPRISE-001")).thenReturn(Optional.of(enterpriseSubscription()));
        when(orderRepository.findByCustomerId(CUSTOMER_ID))
                .thenReturn(List.of(processedSubscriptionOrder("SUB-BASIC-001")));

        // Act & Assert
        assertThat(process("SUB-ENTERPRISE-001").failureReason()).isEqualTo(FailureReason.INCOMPATIBLE_SUBSCRIPTIONS);
    }
}
