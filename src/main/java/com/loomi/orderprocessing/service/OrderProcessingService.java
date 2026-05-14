package com.loomi.orderprocessing.service;

import com.loomi.orderprocessing.model.Order;
import com.loomi.orderprocessing.model.enums.OrderStatus;
import com.loomi.orderprocessing.model.enums.ProductType;
import com.loomi.orderprocessing.service.processor.OrderProcessor;
import com.loomi.orderprocessing.service.processor.ProcessingResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class OrderProcessingService {

    private final Map<ProductType, OrderProcessor> processors;

    public OrderProcessingService(List<OrderProcessor> processorList) {
        this.processors = new HashMap<>();

        for (OrderProcessor processor : processorList) {
            this.processors.put(
                    processor.getSupportedType(),
                    processor
            );
        }
    }

    public OrderStatus process(Order order) {
        boolean hasPendingApproval = false;

        for (var item : order.getItems()) {
            var processor = processors.get(item.getProductType());

            if (processor == null) {
                log.warn("No processor found for type {}", item.getProductType());
                continue;
            }

            ProcessingResult result = processor.process(item, order.getCustomerId(), order);

            if (!result.success()) {
                order.setFailureReason(result.failureReason().name());
                return OrderStatus.FAILED;
            }

            if (result.pendingApproval()) {
                hasPendingApproval = true;
            }

            if (result.metadata() != null) {
                item.setMetadata(result.metadata());
            }
        }

        return hasPendingApproval ? OrderStatus.PENDING_APPROVAL : OrderStatus.PROCESSED;
    }
}
