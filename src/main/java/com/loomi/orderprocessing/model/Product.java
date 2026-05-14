package com.loomi.orderprocessing.model;

import com.loomi.orderprocessing.model.enums.ProductType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Map;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
public class Product {

    @Id
    private String productId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductType productType;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    private Integer stockQty;

    @Column(nullable = false)
    private Boolean active = true;

    @Convert(converter = MetadataConverter.class)
    @Column(columnDefinition = "TEXT")
    private Map<String, Object> metadata;
}
