package com.loomi.orderprocessing.repository;

import com.loomi.orderprocessing.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, String> {}
