package com.yourco.compute.orchestrator.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

/**
 * Signs an IO capability with a secret the caller does not have.
 *
 * <p>The job id, the operation and the expiry are all covered, so a download link cannot be edited
 * into an upload link, pointed at another job, or given a later deadline. Nothing here is derivable
 * from the request, which is the whole point: the ownership check on the endpoint only means
 * something if the capability it guards cannot be reconstructed without it.
 */
@Component
public class StorageSignature {
  private static final int MIN_SECRET_BYTES = 32;
  private static final String ALGORITHM = "HmacSHA256";

  private final SecretKeySpec key;

  public StorageSignature(@Value("${storage.signing-secret:#{null}}") String secret) {
    if (secret == null || secret.isBlank()) {
      throw new IllegalStateException(
          "Storage signing secret is not configured. Set STORAGE_SIGNING_SECRET or the "
              + "storage.signing-secret property, or run with the dev profile.");
    }
    byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
    if (keyBytes.length < MIN_SECRET_BYTES) {
      throw new IllegalStateException("Storage signing secret is " + keyBytes.length
          + " bytes. At least " + MIN_SECRET_BYTES + " are needed.");
    }
    this.key = new SecretKeySpec(keyBytes, ALGORITHM);
  }

  public String sign(long jobId, Operation operation, Instant expiresAt) {
    byte[] mac = mac(canonical(jobId, operation, expiresAt));
    return Base64.getUrlEncoder().withoutPadding().encodeToString(mac);
  }

  /** Constant time, so a caller cannot narrow a signature down one byte at a time. */
  public boolean matches(long jobId, Operation operation, Instant expiresAt, String candidate) {
    if (candidate == null) {
      return false;
    }
    byte[] expected = mac(canonical(jobId, operation, expiresAt));
    byte[] offered;
    try {
      offered = Base64.getUrlDecoder().decode(candidate);
    } catch (IllegalArgumentException notBase64) {
      return false;
    }
    return MessageDigest.isEqual(expected, offered);
  }

  /**
   * Newline separated so the fields stay unambiguous. Today the alphabetic operation already keeps
   * the two numeric fields apart, so the separators are what keeps that true if a field ever widens.
   */
  private static String canonical(long jobId, Operation operation, Instant expiresAt) {
    return jobId + "\n" + operation.name() + "\n" + expiresAt.getEpochSecond();
  }

  private byte[] mac(String canonical) {
    try {
      Mac mac = Mac.getInstance(ALGORITHM);
      mac.init(key);
      return mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8));
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("Cannot sign storage URLs with " + ALGORITHM, e);
    }
  }

  public enum Operation { UPLOAD, DOWNLOAD }
}
