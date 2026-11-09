package com.vandor.repository;

import com.vandor.models.OrderItem;
import jakarta.persistence.EntityManager;

public class OrderItemRepository extends Repository<OrderItem, com.vandor.models.OrderItemId> {

  public OrderItemRepository(EntityManager em) {
    super(em, OrderItem.class);
  }
}
