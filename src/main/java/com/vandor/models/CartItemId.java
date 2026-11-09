package com.vandor.models;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class CartItemId implements Serializable {
  private Long userId;
  private Long productId;

  public CartItemId() {}

  public CartItemId(Long userId, Long productId) {
    this.userId = userId;
    this.productId = productId;
  }

  public Long getUserId() {
    return userId;
  }

  public Long getProductId() {
    return productId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CartItemId that = (CartItemId) o;
    return Objects.equals(userId, that.userId) && Objects.equals(productId, that.productId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, productId);
  }
}
