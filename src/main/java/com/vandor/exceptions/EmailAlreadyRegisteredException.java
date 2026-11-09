package com.vandor.exceptions;

public class EmailAlreadyRegisteredException extends RuntimeException {
  public EmailAlreadyRegisteredException() {
    super("This email is already registered. Please sign in instead.");
  }

  public EmailAlreadyRegisteredException(Throwable cause) {
    super("This email is already registered. Please sign in instead.", cause);
  }
}
