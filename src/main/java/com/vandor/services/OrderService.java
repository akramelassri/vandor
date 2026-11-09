package com.vandor.services;

import com.vandor.exceptions.AccessDeniedException;
import com.vandor.models.CartItem;
import com.vandor.models.CustomerUser;
import com.vandor.models.Order;
import com.vandor.models.OrderItem;
import com.vandor.models.OrderItemId;
import com.vandor.models.OrderStatus;
import com.vandor.models.Product;
import com.vandor.models.User;
import com.vandor.models.UserRole;
import com.vandor.models.UserStatus;
import com.vandor.repository.CartItemRepository;
import com.vandor.repository.OrderRepository;
import com.vandor.repository.ProductRepository;
import com.vandor.repository.UserRepository;
import com.vandor.util.Permissions;
import com.vandor.util.TransactionManager;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OrderService {
  private final TransactionManager db;

  public OrderService() {
    this(new TransactionManager());
  }

  public OrderService(TransactionManager db) {
    this.db = db;
  }

  public List<Long> checkout(Long customer) {
    return db.execute(em -> {
      Permissions.require(new UserRepository(em).lock(customer), UserRole.CUSTOMER);
      List<CartItem> cart = new CartItemRepository(em).forUser(customer);
      if (cart.isEmpty()) {
        throw new IllegalArgumentException(
            "Your cart is empty or this checkout has already been placed.");
      }
      ProductRepository products = new ProductRepository(em);
      for (CartItem item : cart) {
        Product p = products.lock(item.getProduct().getId());
        if (p.getSeller().getUser().getStatus() != UserStatus.ACTIVE
            || item.getQuantity() > p.getStock()) {
          throw new IllegalArgumentException(
              "Stock has changed. Adjust your cart to the available stock before placing your"
                  + " order.");
        }
      }
      Map<Long, Order> orders = new LinkedHashMap<>();
      for (CartItem item : cart) {
        Product p = item.getProduct();
        Long seller = p.getSeller().getSellerId();
        Order order = orders.get(seller);
        if (order == null) {
          order = new Order();
          order.setCustomer(em.getReference(CustomerUser.class, customer));
          em.persist(order);
          orders.put(seller, order);
        }
        OrderItem line = new OrderItem();
        OrderItemId id = new OrderItemId();
        id.setOrderId(order.getId());
        id.setProductId(p.getId());
        line.setId(id);
        line.setOrder(order);
        line.setProduct(p);
        line.setQuantity(item.getQuantity());
        em.persist(line);
        p.setStock(p.getStock() - item.getQuantity());
      }
      new CartItemRepository(em).clear(customer);
      return orders.values().stream().map(Order::getId).toList();
    });
  }

  public List<Order> list(User user) {
    return db.execute(em -> {
      User current = user == null ? null : em.find(User.class, user.getId());
      if (current == null || current.getStatus() != UserStatus.ACTIVE) {
        throw new AccessDeniedException();
      }
      return new OrderRepository(em)
          .list(
              current.getRole() == UserRole.CUSTOMER ? current.getId() : null,
              current.getRole() == UserRole.SELLER ? current.getId() : null);
    });
  }

  public void change(Long actor, Long id, OrderStatus next) {
    db.execute(em -> {
      User user = new UserRepository(em).lock(actor);
      OrderRepository repo = new OrderRepository(em);
      Order order = repo.lock(id);
      authorizeChange(user, order, next);
      if (!order.getStatus().canTransitionTo(next)) {
        throw new IllegalArgumentException(
            "This order cannot make that status change. Refresh to see its current status.");
      }
      if (next == OrderStatus.CANCELLED) {
        for (OrderItem line :
            order.getItems().stream()
                .sorted(Comparator.comparing(i -> i.getProduct().getId()))
                .toList()) {
          Product p = new ProductRepository(em).lock(line.getProduct().getId());
          try {
            p.setStock(Math.addExact(p.getStock(), line.getQuantity()));
          } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Stock is too large to restore this order.");
          }
        }
      }
      order.setStatus(next);
      return null;
    });
  }

  private void authorizeChange(User user, Order order, OrderStatus next) {
    switch (user.getRole()) {
      case ADMIN -> {}
      case CUSTOMER -> {
        if (!order.getCustomer().getCustomerId().equals(user.getId())
            || next != OrderStatus.CANCELLED) {
          throw new AccessDeniedException();
        }
        if (order.getStatus() != OrderStatus.PENDING) {
          throw new IllegalArgumentException(
              "Only pending orders can be cancelled by the customer.");
        }
      }
      case SELLER -> {
        boolean ownsAllLines =
            !order.getItems().isEmpty()
                && order.getItems().stream()
                    .allMatch(
                        line -> line.getProduct().getSeller().getSellerId().equals(user.getId()));
        if (!ownsAllLines) {
          throw new AccessDeniedException();
        }
      }
    }
  }
}
