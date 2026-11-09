package com.vandor.exceptions;

public class AccessDeniedException extends RuntimeException {
  public AccessDeniedException() {
    super("You do not have access to this action.");
  }
}
