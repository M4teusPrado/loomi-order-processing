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

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static com.loomi.orderprocessing.fixture.OrderMock.buildItem;
import static com.loomi.orderprocessing.fixture.ProductMock.build;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CorporateOrderProcessorTest {

    @Mock private ProductRepository productRepository;
    @InjectMocks private CorporateOrderProcessor processor;

    private static final String CUSTOMER_ID = "customer-corp";
    private static final String PRODUCT_ID = "CORP-LICENSE-ENT";
    private static final String VALID_CNPJ = "12345678000195";

    private void mockProduct(String cnpj, BigDecimal price) {
        var product = build(PRODUCT_ID, "Enterprise License", ProductType.CORPORATE);
        product.setPrice(price);
        product.setMetadata(cnpj != null ? Map.of("cnpj", cnpj) : null);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
    }

    private ProcessingResult process(int quantity) {
        var item = buildItem(PRODUCT_ID, ProductType.CORPORATE, quantity);
        item.setUnitPrice(productRepository.findById(PRODUCT_ID).get().getPrice());
        return processor.process(item, CUSTOMER_ID, new Order());
    }

    @Test
    void shouldReturnSuccessForValidOrder() {
        // Arrange
        mockProduct(VALID_CNPJ, new BigDecimal("100.00"));

        // Act
        var result = process(1);

        // Assert
        assertThat(result.success()).isTrue();
    }

    @Test
    void shouldFailWhenCnpjIsInvalid() {
        // Arrange
        mockProduct("123", new BigDecimal("100.00"));

        // Act & Assert
        assertThat(process(1).failureReason()).isEqualTo(FailureReason.INVALID_CORPORATE_DATA);
    }

    @Test
    void shouldFailWhenCreditLimitExceeded() {
        // Arrange
        mockProduct(VALID_CNPJ, new BigDecimal("101000.00"));

        // Act & Assert
        assertThat(process(1).failureReason()).isEqualTo(FailureReason.CREDIT_LIMIT_EXCEEDED);
    }

    @Test
    void shouldApplyVolumeDiscountWhenQuantityAbove100() {
        // Arrange
        mockProduct(VALID_CNPJ, new BigDecimal("100.00"));

        // Act
        var result = process(101);

        // Assert
        assertThat(result.success()).isTrue();
        assertThat(result.metadata()).containsEntry("discount", 0.15);
    }

    @Test
    void shouldReturnPendingApprovalWhenSubtotalAbove50k() {
        // Arrange
        mockProduct(VALID_CNPJ, new BigDecimal("51000.00"));

        // Act
        var result = process(1);

        // Assert
        assertThat(result.success()).isTrue();
        assertThat(result.pendingApproval()).isTrue();
    }
}
