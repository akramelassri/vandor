package com.vandor.repository;

import com.vandor.models.Product;
import jakarta.persistence.EntityManager;

public class ProductRepository extends Repository<Product, Long> {

    public ProductRepository(EntityManager em) {
        super(em, Product.class);
    }
}
