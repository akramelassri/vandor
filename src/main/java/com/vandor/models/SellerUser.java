package com.vandor.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "seller_users")
public class SellerUser {
  @Id
  @Column(name = "seller_id")
  private Long sellerId;

  @OneToOne
  @MapsId
  @JoinColumn(name = "seller_id")
  private User user;

  public SellerUser(User user) {
    this.user = user;
  }

  // For JPA
  protected SellerUser() {}

  public Long getSellerId() {
    return sellerId;
  }

  public User getUser() {
    return user;
  }
}
