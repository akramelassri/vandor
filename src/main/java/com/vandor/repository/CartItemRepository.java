package com.vandor.repository;

import com.vandor.models.CartItem;
import jakarta.persistence.EntityManager;

public class CartItemRepository extends Repository<CartItem, Long> {

    public CartItemRepository(EntityManager em) {
        super(em, CartItem.class);
    }
}
