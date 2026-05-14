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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SubscriptionOrderProcessor implements OrderProcessor {

    private static final String ENTERPRISE = "enterprise";
    private static final String BASIC = "basic";

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Override
    public ProductType getSupportedType() {
        return ProductType.SUBSCRIPTION;
    }

    @Override
    public ProcessingResult process(OrderItem item, String customerId, Order order) {
        var activeItems = getActiveSubscriptionItems(customerId);

        if (alreadySubscribed(item.getProductId(), activeItems)) {
            return ProcessingResult.fail(FailureReason.DUPLICATE_ACTIVE_SUBSCRIPTION);
        }

        if (activeItems.size() >= 5) {
            return ProcessingResult.fail(FailureReason.SUBSCRIPTION_LIMIT_EXCEEDED);
        }

        if (hasIncompatiblePlan(item.getProductId(), activeItems)) {
            return ProcessingResult.fail(FailureReason.INCOMPATIBLE_SUBSCRIPTIONS);
        }

        return ProcessingResult.ok(Map.of("subscriptionStart", LocalDate.now().toString()));
    }

    private boolean alreadySubscribed(String productId, List<OrderItem> activeItems) {
        return activeItems.stream().anyMatch(i -> i.getProductId().equals(productId));
    }

    private boolean hasIncompatiblePlan(String newProductId, List<OrderItem> activeItems) {
        boolean isEnterprise = isEnterprisePlan(newProductId);
        boolean isBasic = isBasicPlan(newProductId);

        boolean activeHasEnterprise = activeItems.stream().anyMatch(i -> isEnterprisePlan(i.getProductId()));
        boolean activeHasBasic = activeItems.stream().anyMatch(i -> isBasicPlan(i.getProductId()));

        return (isEnterprise && activeHasBasic) || (isBasic && activeHasEnterprise);
    }

    private boolean isEnterprisePlan(String productId) {
        return planNameContains(productId, ENTERPRISE);
    }

    private boolean isBasicPlan(String productId) {
        return planNameContains(productId, BASIC);
    }

    private boolean planNameContains(String productId, String keyword) {
        return productRepository.findById(productId)
                .map(p -> p.getName().toLowerCase().contains(keyword))
                .orElse(false);
    }

    private List<OrderItem> getActiveSubscriptionItems(String customerId) {
        List<OrderItem> result = new ArrayList<>();

        for (Order order : orderRepository.findByCustomerId(customerId)) {
            if (order.getStatus() == OrderStatus.PROCESSED) result.addAll(order.getItems());
        }

        return result.stream()
                .filter(i -> i.getProductType() == ProductType.SUBSCRIPTION)
                .toList();
    }
}
