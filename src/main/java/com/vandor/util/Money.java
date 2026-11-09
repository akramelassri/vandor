package com.vandor.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Money {
  private Money() {}

  public static String format(long cents) {
    return "€" + BigDecimal.valueOf(cents, 2).toPlainString();
  }

  public static int parse(String value) {
    try {
      return new BigDecimal(value)
          .movePointRight(2)
          .setScale(0, RoundingMode.UNNECESSARY)
          .intValueExact();
    } catch (RuntimeException e) {
      throw new IllegalArgumentException("Enter a valid price with at most two decimal places.");
    }
  }
}
