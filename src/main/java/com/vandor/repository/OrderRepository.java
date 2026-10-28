package com.vandor.repository;

import com.vandor.models.Order;
import jakarta.persistence.EntityManager;

public class OrderRepository extends Repository<Order, Long> {

    public OrderRepository(EntityManager em) {
        super(em, Order.class);
    }
}
