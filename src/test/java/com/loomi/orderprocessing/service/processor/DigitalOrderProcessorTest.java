package com.loomi.orderprocessing.service.processor;

import com.loomi.orderprocessing.model.Order;
import com.loomi.orderprocessing.model.enums.FailureReason;
import com.loomi.orderprocessing.model.enums.ProductType;
import com.loomi.orderprocessing.repository.OrderRepository;
import com.loomi.orderprocessing.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.loomi.orderprocessing.fixture.OrderMock.*;
import static com.loomi.orderprocessing.fixture.ProductMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DigitalOrderProcessorTest {

    @Mock private OrderRepository orderRepository;
    @Mock private ProductRepository productRepository;
    @InjectMocks private DigitalOrderProcessor processor;

    private static final String CUSTOMER_ID = "customer-123";
    private static final String PRODUCT_ID = "EBOOK-JAVA-001";

    private ProcessingResult process() {
        return processor.process(buildItem(PRODUCT_ID, ProductType.DIGITAL), CUSTOMER_ID, new Order());
    }

    @Test
    void shouldReturnSuccessWithActivationKey() {
        // Arrange
        var product = effectiveJava();
        product.setMetadata(new HashMap<>(Map.of("licenses", 100)));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(orderRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(List.of());

        // Act
        var result = process();

        // Assert
        assertThat(result.success()).isTrue();
        assertThat(result.metadata()).containsKey("activationKey");
    }

    @Test
    void shouldFailWhenNoLicensesAvailable() {
        // Arrange
        var product = effectiveJava();
        product.setMetadata(new HashMap<>(Map.of("licenses", 0)));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

        // Act & Assert
        assertThat(process().failureReason()).isEqualTo(FailureReason.LICENSE_UNAVAILABLE);
    }

    @Test
    void shouldFailWhenCustomerAlreadyOwnsProduct() {
        // Arrange
        var product = effectiveJava();
        product.setMetadata(new HashMap<>(Map.of("licenses", 100)));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(orderRepository.findByCustomerId(CUSTOMER_ID))
                .thenReturn(List.of(buildProcessedOrderWith(PRODUCT_ID, ProductType.DIGITAL)));

        // Act & Assert
        assertThat(process().failureReason()).isEqualTo(FailureReason.ALREADY_OWNED);
    }
}
