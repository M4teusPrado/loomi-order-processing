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

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.loomi.orderprocessing.fixture.OrderMock.buildItem;
import static com.loomi.orderprocessing.fixture.ProductMock.build;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PreOrderProcessorTest {

    @Mock private ProductRepository productRepository;
    @InjectMocks private PreOrderProcessor processor;

    private static final String CUSTOMER_ID = "customer-123";
    private static final String PRODUCT_ID = "GAME-2025-001";

    private ProcessingResult process() {
        return processor.process(buildItem(PRODUCT_ID, ProductType.PRE_ORDER), CUSTOMER_ID, new Order());
    }

    private void mockProduct(String releaseDate, int slots) {
        var product = build(PRODUCT_ID, "Epic Game 2027", ProductType.PRE_ORDER);
        product.setMetadata(new HashMap<>(Map.of("releaseDate", releaseDate, "preOrderSlots", slots)));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
    }

    @Test
    void shouldReturnSuccessWithReleaseDateAndSlotConfirmed() {
        // Arrange
        mockProduct(LocalDate.now().plusMonths(6).toString(), 100);

        // Act
        var result = process();

        // Assert
        assertThat(result.success()).isTrue();
        assertThat(result.metadata()).containsKeys("releaseDate", "slotConfirmed");
    }

    @Test
    void shouldFailWhenReleaseDateHasPassed() {
        // Arrange
        mockProduct(LocalDate.now().minusDays(1).toString(), 100);

        // Act & Assert
        assertThat(process().failureReason()).isEqualTo(FailureReason.RELEASE_DATE_PASSED);
    }

    @Test
    void shouldFailWhenNoSlotsAvailable() {
        // Arrange
        mockProduct(LocalDate.now().plusMonths(6).toString(), 0);

        // Act & Assert
        assertThat(process().failureReason()).isEqualTo(FailureReason.PRE_ORDER_SOLD_OUT);
    }
}
