package com.yourco.compute.api.security;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Turns the configured secret into an HS256 key, rejecting anything the algorithm cannot use.
 * A short secret would otherwise let the application start and then answer 401 to every request,
 * leaving the operator to diagnose a configuration mistake from traffic instead of from startup.
 */
final class JwtSecret {
  static final int MIN_BYTES = 32;
  private static final String ALGORITHM = "HmacSHA256";

  private JwtSecret() {}

  static SecretKeySpec toKey(String secret) {
    if (secret == null || secret.isBlank()) {
      throw new IllegalStateException(
          "JWT secret is not configured. Set the JWT_SECRET environment variable or the "
              + "security.jwt.secret property, or run with the dev profile.");
    }
    byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
    if (keyBytes.length < MIN_BYTES) {
      throw new IllegalStateException(
          "JWT secret is " + keyBytes.length + " bytes. HS256 needs at least " + MIN_BYTES + ".");
    }
    return new SecretKeySpec(keyBytes, ALGORITHM);
  }
}
