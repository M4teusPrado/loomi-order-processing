package com.loomi.orderprocessing.service.processor;

import com.loomi.orderprocessing.model.Order;
import com.loomi.orderprocessing.model.enums.FailureReason;
import com.loomi.orderprocessing.model.enums.ProductType;
import com.loomi.orderprocessing.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.loomi.orderprocessing.fixture.OrderMock.buildItem;
import static com.loomi.orderprocessing.fixture.ProductMock.laptopWithStock;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PhysicalOrderProcessorTest {

    @Mock private ProductRepository productRepository;
    @InjectMocks private PhysicalOrderProcessor processor;

    private static final String PRODUCT_ID = "LAPTOP-PRO-2024";
    private static final String CUSTOMER_ID = "customer-001";

    @Test
    void shouldProcessSuccessfullyWhenStockIsSufficient() {
        // Arrange
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(laptopWithStock(10)));

        // Act
        var result = processor.process(buildItem(PRODUCT_ID, ProductType.PHYSICAL), CUSTOMER_ID, new Order());

        // Assert
        assertThat(result.success()).isTrue();
        assertThat(result.metadata()).containsKey("deliveryDays");
    }

    @Test
    void shouldFailWhenStockIsInsufficient() {
        // Arrange
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(laptopWithStock(0)));

        // Act
        var result = processor.process(buildItem(PRODUCT_ID, ProductType.PHYSICAL, 1), CUSTOMER_ID, new Order());

        // Assert
        assertThat(result.success()).isFalse();
        assertThat(result.failureReason()).isEqualTo(FailureReason.OUT_OF_STOCK);
    }

    @Test
    void shouldFailWhenRequestedQuantityExceedsStock() {
        // Arrange
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(laptopWithStock(3)));

        // Act
        var result = processor.process(buildItem(PRODUCT_ID, ProductType.PHYSICAL, 5), CUSTOMER_ID, new Order());

        // Assert
        assertThat(result.success()).isFalse();
        assertThat(result.failureReason()).isEqualTo(FailureReason.OUT_OF_STOCK);
    }

    @Test
    void shouldDecrementStockAfterSuccessfulProcessing() {
        // Arrange
        var product = laptopWithStock(10);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

        // Act
        processor.process(buildItem(PRODUCT_ID, ProductType.PHYSICAL, 3), CUSTOMER_ID, new Order());

        // Assert
        assertThat(product.getStockQty()).isEqualTo(7);
    }
}
