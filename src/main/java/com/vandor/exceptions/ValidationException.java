package com.vandor.exceptions;

import java.util.Map;

public class ValidationException extends RuntimeException {
  private final Map<String, String> errors;

  public ValidationException(Map<String, String> errors) {
    super("Please correct the registration fields.");
    this.errors = Map.copyOf(errors);
  }

  public Map<String, String> getErrors() {
    return errors;
  }
}
