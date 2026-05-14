package com.loomi.orderprocessing.exception;

public class ProductNotAvailableException extends RuntimeException {
    public ProductNotAvailableException(String productId) {
        super("Product not available: " + productId);
    }
}
