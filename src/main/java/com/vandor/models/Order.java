package com.vandor.models;

import com.vandor.util.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "customer_id")
  private CustomerUser customer;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "status", nullable = false)
  @Enumerated(EnumType.STRING)
  private OrderStatus status = OrderStatus.PENDING;

  @OneToMany(mappedBy = "order")
  @OrderBy("id.productId")
  private List<OrderItem> items = new ArrayList<>();

  public Order() {}

  public Long getId() {
    return id;
  }

  public CustomerUser getCustomer() {
    return customer;
  }

  public void setCustomer(CustomerUser value) {
    customer = value;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public String getDate() {
    return DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")
        .withZone(ZoneOffset.UTC)
        .format(createdAt);
  }

  public OrderStatus getStatus() {
    return status;
  }

  public void setStatus(OrderStatus value) {
    status = value;
  }

  public List<OrderItem> getItems() {
    return items;
  }

  public String getFormattedTotal() {
    return Money.format(
        items.stream().mapToLong(i -> (long) i.getProduct().getPrice() * i.getQuantity()).sum());
  }

  public boolean isMixedSeller() {
    return items.stream().map(i -> i.getProduct().getSeller().getSellerId()).distinct().count() > 1;
  }
}
