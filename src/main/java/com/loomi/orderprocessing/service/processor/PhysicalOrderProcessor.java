package com.loomi.orderprocessing.service.processor;

import com.loomi.orderprocessing.model.Order;
import com.loomi.orderprocessing.model.OrderItem;
import com.loomi.orderprocessing.model.enums.FailureReason;
import com.loomi.orderprocessing.model.enums.ProductType;
import com.loomi.orderprocessing.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
public class PhysicalOrderProcessor implements OrderProcessor {

    private final ProductRepository productRepository;

    @Override
    public ProductType getSupportedType() {
        return ProductType.PHYSICAL;
    }

    @Override
    public ProcessingResult process(OrderItem item, String customerId, Order order) {
        var product = productRepository.findById(item.getProductId()).orElseThrow();

        if (product.getStockQty() == null || product.getStockQty() < item.getQuantity()) {
            return ProcessingResult.fail(FailureReason.OUT_OF_STOCK);
        }

        int remaining = product.getStockQty() - item.getQuantity();
        product.setStockQty(remaining);
        productRepository.save(product);

        if (remaining < 5) {
            log.warn("LOW_STOCK: product {} has only {} units left", product.getProductId(), remaining);
        }

        int deliveryDays = resolveDeliveryDays();

        return ProcessingResult.ok(Map.of("deliveryDays", deliveryDays));
    }

    private int resolveDeliveryDays() {
        int[] options = {5, 7, 10};
        return options[new Random().nextInt(options.length)];
    }
}
