package com.yourco.compute.api.security;

public class UnauthorizedCallerException extends RuntimeException {
  public UnauthorizedCallerException(String message) {
    super(message);
  }
}
