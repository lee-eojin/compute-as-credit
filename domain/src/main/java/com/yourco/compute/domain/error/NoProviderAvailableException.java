package com.yourco.compute.domain.error;

public class NoProviderAvailableException extends RuntimeException {
  public NoProviderAvailableException(String region, String gpuType) {
    super("No provider available for " + gpuType + " in " + region);
  }
}
