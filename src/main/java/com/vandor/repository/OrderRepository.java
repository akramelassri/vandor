package com.vandor.repository;

import com.vandor.models.Order;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.LockModeType;
import java.util.List;

public class OrderRepository extends Repository<Order, Long> {
  public OrderRepository(EntityManager em) {
    super(em, Order.class);
  }

  public List<Order> list(Long customer, Long seller) {
    String filter = "";
    Long owner = null;
    if (customer != null) {
      filter = " WHERE o.customer.customerId = :owner";
      owner = customer;
    } else if (seller != null) {
      filter =
          """
           WHERE EXISTS (SELECT line.id FROM OrderItem line
             WHERE line.order = o AND line.product.seller.sellerId = :owner)
          """;
      owner = seller;
    }
    var query =
        em.createQuery(
            """
            SELECT DISTINCT o FROM Order o
            JOIN FETCH o.customer customer JOIN FETCH customer.user
            LEFT JOIN FETCH o.items item LEFT JOIN FETCH item.product product
            LEFT JOIN FETCH product.seller seller LEFT JOIN FETCH seller.user
            """
                + filter
                + " ORDER BY o.id DESC",
            Order.class);
    if (owner != null) {
      query.setParameter("owner", owner);
    }
    return query.getResultList();
  }

  public Order lock(Long id) {
    Order order = em.find(Order.class, id, LockModeType.PESSIMISTIC_WRITE);
    if (order == null) {
      throw new EntityNotFoundException("Order not found.");
    }
    return order;
  }
}
