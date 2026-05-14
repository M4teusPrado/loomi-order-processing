package com.loomi.orderprocessing.service.processor;

import com.loomi.orderprocessing.model.Order;
import com.loomi.orderprocessing.model.OrderItem;
import com.loomi.orderprocessing.model.enums.FailureReason;
import com.loomi.orderprocessing.model.enums.OrderStatus;
import com.loomi.orderprocessing.model.enums.ProductType;
import com.loomi.orderprocessing.repository.OrderRepository;
import com.loomi.orderprocessing.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DigitalOrderProcessor implements OrderProcessor {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Override
    public ProductType getSupportedType() {
        return ProductType.DIGITAL;
    }

    @Override
    public ProcessingResult process(OrderItem item, String customerId, Order order) {
        var product = productRepository.findById(item.getProductId()).orElseThrow();

        if (! hasAvailableLicenses(product.getMetadata())) {
            return ProcessingResult.fail(FailureReason.LICENSE_UNAVAILABLE);
        }

        if (customerAlreadyOwns(customerId, item.getProductId())) {
            return ProcessingResult.fail(FailureReason.ALREADY_OWNED);
        }

        decrementLicenses(item.getProductId(), product.getMetadata());

        String activationKey = UUID.randomUUID().toString();
        return ProcessingResult.ok(Map.of("activationKey", activationKey));
    }

    private boolean hasAvailableLicenses(Map<String, Object> metadata) {
        if (metadata == null || !metadata.containsKey("licenses")) return false;
        int licenses = ((Number) metadata.get("licenses")).intValue();
        return licenses > 0;
    }

    private boolean customerAlreadyOwns(String customerId, String productId) {
        return orderRepository.findByCustomerId(customerId).stream()
                .filter(o -> o.getStatus() == OrderStatus.PROCESSED)
                .flatMap(o -> o.getItems().stream())
                .anyMatch(i -> i.getProductType() == ProductType.DIGITAL
                        && i.getProductId().equals(productId));
    }

    private void decrementLicenses(String productId, Map<String, Object> metadata) {
        int current = ((Number) metadata.get("licenses")).intValue();
        metadata.put("licenses", current - 1);
        productRepository.findById(productId).ifPresent(p -> {
            p.setMetadata(metadata);
            productRepository.save(p);
        });
    }
}
