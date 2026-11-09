package com.vandor.dto;

import com.vandor.models.Product;
import com.vandor.models.UserStatus;
import com.vandor.util.Money;

public class CartLine {
  private final Product product;
  private final int quantity;

  public CartLine(Product product, int quantity) {
    this.product = product;
    this.quantity = quantity;
  }

  public Product getProduct() {
    return product;
  }

  public int getQuantity() {
    return quantity;
  }

  public boolean isChanged() {
    return quantity > product.getStock()
        || product.getSeller().getUser().getStatus() != UserStatus.ACTIVE;
  }

  public String getFormattedSubtotal() {
    return Money.format((long) product.getPrice() * quantity);
  }
}
