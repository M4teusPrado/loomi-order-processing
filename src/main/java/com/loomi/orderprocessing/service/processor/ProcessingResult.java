package com.loomi.orderprocessing.service.processor;

import com.loomi.orderprocessing.model.enums.FailureReason;

import java.util.Map;

public record ProcessingResult(
        boolean success,
        boolean pendingApproval,
        FailureReason failureReason,
        Map<String, Object> metadata
) {
    public static ProcessingResult ok(Map<String, Object> metadata) {
        return new ProcessingResult(true, false, null, metadata);
    }

    public static ProcessingResult pendingApproval(Map<String, Object> metadata) {
        return new ProcessingResult(true, true, null, metadata);
    }

    public static ProcessingResult fail(FailureReason reason) {
        return new ProcessingResult(false, false, reason, null);
    }
}
