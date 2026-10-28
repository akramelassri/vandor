package com.vandor.repository;

import com.vandor.models.OrderItem;
import jakarta.persistence.EntityManager;

public class OrderItemRepository extends Repository<OrderItem, Long> {

    public OrderItemRepository(EntityManager em) {
        super(em, OrderItem.class);
    }
}
