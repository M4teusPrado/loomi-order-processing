package com.loomi.orderprocessing.service.processor;

import com.loomi.orderprocessing.model.Order;
import com.loomi.orderprocessing.model.OrderItem;
import com.loomi.orderprocessing.model.enums.FailureReason;
import com.loomi.orderprocessing.model.enums.ProductType;
import com.loomi.orderprocessing.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CorporateOrderProcessor implements OrderProcessor {

    private static final String CNPJ_KEY = "cnpj";
    private static final BigDecimal CREDIT_LIMIT = new BigDecimal("100000");
    private static final BigDecimal APPROVAL_THRESHOLD = new BigDecimal("50000");
    private static final BigDecimal PAYMENT_TERM_60_THRESHOLD = new BigDecimal("10000");
    private static final int VOLUME_DISCOUNT_MIN_QTY = 100;
    private static final double VOLUME_DISCOUNT_RATE = 0.15;

    private final ProductRepository productRepository;

    @Override
    public ProductType getSupportedType() {
        return ProductType.CORPORATE;
    }

    @Override
    public ProcessingResult process(OrderItem item, String customerId, Order order) {
        var product = productRepository.findById(item.getProductId()).orElseThrow();

        if (!hasValidCnpj(product.getMetadata())) {
            return ProcessingResult.fail(FailureReason.INVALID_CORPORATE_DATA);
        }

        BigDecimal subtotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));

        if (exceedsCreditLimit(subtotal)) {
            return ProcessingResult.fail(FailureReason.CREDIT_LIMIT_EXCEEDED);
        }

        Map<String, Object> metadata = new HashMap<>();

        if (item.getQuantity() > VOLUME_DISCOUNT_MIN_QTY) {
            BigDecimal discountedSubtotal = subtotal.multiply(BigDecimal.valueOf(1 - VOLUME_DISCOUNT_RATE));
            metadata.put("discount", VOLUME_DISCOUNT_RATE);
            metadata.put("discountedSubtotal", discountedSubtotal);
            subtotal = discountedSubtotal;
        }

        metadata.put("paymentTermDays", resolvePaymentTerm(subtotal));

        if (subtotal.compareTo(APPROVAL_THRESHOLD) > 0) {
            return ProcessingResult.pendingApproval(metadata);
        }

        return ProcessingResult.ok(metadata);
    }

    private int resolvePaymentTerm(BigDecimal subtotal) {
        if (subtotal.compareTo(APPROVAL_THRESHOLD) > 0) return 90;
        if (subtotal.compareTo(PAYMENT_TERM_60_THRESHOLD) > 0) return 60;
        return 30;
    }

    private boolean exceedsCreditLimit(BigDecimal subtotal) {
        return subtotal.compareTo(CREDIT_LIMIT) > 0;
    }

    private boolean hasValidCnpj(Map<String, Object> productMetadata) {
        if (productMetadata == null || !productMetadata.containsKey(CNPJ_KEY)) return false;
        String cnpj = productMetadata.get(CNPJ_KEY).toString().replaceAll("[^0-9]", "");
        return cnpj.length() == 14;
    }
}
