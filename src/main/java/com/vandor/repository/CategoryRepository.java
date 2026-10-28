package com.vandor.repository;

import com.vandor.models.Category;
import jakarta.persistence.EntityManager;

public class CategoryRepository extends Repository<Category, Long> {

    public CategoryRepository(EntityManager em) {
        super(em, Category.class);
    }
}
