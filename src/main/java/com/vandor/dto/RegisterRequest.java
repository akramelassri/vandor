package com.vandor.dto;

public record RegisterRequest(
    String firstName, String lastName, String email, String password, String role) {
  @Override
  public String toString() {
    return "RegisterRequest[password=REDACTED]";
  }
}
