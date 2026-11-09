package com.vandor.models;

public enum OrderStatus {
  PENDING,
  CONFIRMED,
  COMPLETED,
  CANCELLED;

  public boolean canTransitionTo(OrderStatus next) {
    return switch (this) {
      case PENDING -> next == CONFIRMED || next == CANCELLED;
      case CONFIRMED -> next == COMPLETED || next == CANCELLED;
      case COMPLETED, CANCELLED -> false;
    };
  }
}
