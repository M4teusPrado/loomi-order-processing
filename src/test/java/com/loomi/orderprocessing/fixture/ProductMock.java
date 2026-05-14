package com.loomi.orderprocessing.fixture;

import com.loomi.orderprocessing.model.Product;
import com.loomi.orderprocessing.model.enums.ProductType;

import java.math.BigDecimal;

public class ProductMock {

    public static Product build(String productId, String name, ProductType type) {
        var product = new Product();
        product.setProductId(productId);
        product.setName(name);
        product.setProductType(type);
        product.setPrice(BigDecimal.TEN);
        product.setActive(true);
        return product;
    }

    public static Product build(String productId, String name, ProductType type, int stock) {
        var product = build(productId, name, type);
        product.setStockQty(stock);
        return product;
    }

    // Subscription
    public static Product premiumSubscription() {
        return build("SUB-PREMIUM-001", "Premium Monthly", ProductType.SUBSCRIPTION);
    }

    public static Product basicSubscription() {
        return build("SUB-BASIC-001", "Basic Monthly", ProductType.SUBSCRIPTION);
    }

    public static Product enterpriseSubscription() {
        return build("SUB-ENTERPRISE-001", "Enterprise Plan", ProductType.SUBSCRIPTION);
    }

    // Physical
    public static Product laptopWithStock(int stock) {
        return build("LAPTOP-PRO-2024", "Laptop Pro", ProductType.PHYSICAL, stock);
    }

    // Digital
    public static Product effectiveJava() {
        return build("EBOOK-JAVA-001", "Effective Java", ProductType.DIGITAL);
    }
}
