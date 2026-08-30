package com.yourco.compute.domain.error;

import java.util.Set;

public class UnsupportedResourceHintException extends IllegalArgumentException {
  public UnsupportedResourceHintException(String field, String value, Set<String> supported) {
    super("Unsupported " + field + " '" + value + "'. Supported values: " + supported);
  }
}
