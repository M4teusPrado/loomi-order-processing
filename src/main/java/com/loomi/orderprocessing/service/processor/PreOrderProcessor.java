package com.loomi.orderprocessing.service.processor;

import com.loomi.orderprocessing.model.Order;
import com.loomi.orderprocessing.model.OrderItem;
import com.loomi.orderprocessing.model.enums.FailureReason;
import com.loomi.orderprocessing.model.Product;
import com.loomi.orderprocessing.model.enums.ProductType;
import com.loomi.orderprocessing.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PreOrderProcessor implements OrderProcessor {

    private static final String RELEASE_DATE_KEY = "releaseDate";
    private static final String SLOTS_KEY = "preOrderSlots";
    private static final String SLOT_CONFIRMED_KEY = "slotConfirmed";

    private final ProductRepository productRepository;

    @Override
    public ProductType getSupportedType() {
        return ProductType.PRE_ORDER;
    }

    @Override
    public ProcessingResult process(OrderItem item, String customerId, Order order) {
        var product = productRepository.findById(item.getProductId()).orElseThrow();
        var metadata = product.getMetadata();

        LocalDate releaseDate = parseReleaseDate(metadata);
        if (releaseDate == null || !releaseDate.isAfter(LocalDate.now())) {
            return ProcessingResult.fail(FailureReason.RELEASE_DATE_PASSED);
        }

        if (!hasAvailableSlots(metadata)) {
            return ProcessingResult.fail(FailureReason.PRE_ORDER_SOLD_OUT);
        }

        decrementSlots(product);

        return ProcessingResult.ok(Map.of(
                RELEASE_DATE_KEY, releaseDate.toString(),
                SLOT_CONFIRMED_KEY, true
        ));
    }

    private LocalDate parseReleaseDate(Map<String, Object> metadata) {
        try {
            return LocalDate.parse((String) metadata.get(RELEASE_DATE_KEY));
        } catch (Exception e) {
            return null;
        }
    }

    private boolean hasAvailableSlots(Map<String, Object> metadata) {
        if (metadata == null || !metadata.containsKey(SLOTS_KEY)) return false;
        int slots = ((Number) metadata.get(SLOTS_KEY)).intValue();
        return slots > 0;
    }

    private void decrementSlots(Product product) {
        var metadata = product.getMetadata();
        int current = ((Number) metadata.get(SLOTS_KEY)).intValue();
        metadata.put(SLOTS_KEY, current - 1);
        product.setMetadata(metadata);
        productRepository.save(product);
    }
}
