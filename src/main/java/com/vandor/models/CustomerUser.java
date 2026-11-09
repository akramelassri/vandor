package com.vandor.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "customer_users")
public class CustomerUser {
  @Id
  @Column(name = "customer_id")
  private Long customerId;

  @OneToOne
  @MapsId
  @JoinColumn(name = "customer_id")
  private User user;

  public CustomerUser(User user) {
    this.user = user;
  }

  // For JPA
  protected CustomerUser() {}

  public Long getCustomerId() {
    return customerId;
  }

  public User getUser() {
    return user;
  }
}
