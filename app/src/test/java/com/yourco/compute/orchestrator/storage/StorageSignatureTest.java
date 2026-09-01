package com.yourco.compute.orchestrator.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static com.yourco.compute.orchestrator.storage.StorageSignature.Operation.DOWNLOAD;
import static com.yourco.compute.orchestrator.storage.StorageSignature.Operation.UPLOAD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StorageSignatureTest {
  private static final Instant EXPIRY = Instant.parse("2026-01-01T00:00:00Z");

  private final StorageSignature signature = new StorageSignature("a-secret-that-is-long-enough-to-sign");

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  void anUnsetSecretStopsStartup(String secret) {
    assertThatThrownBy(() -> new StorageSignature(secret))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("STORAGE_SIGNING_SECRET");
  }

  @Test
  void aSecretTooShortToBeWorthAnythingIsRejected() {
    assertThatThrownBy(() -> new StorageSignature("short"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("At least 32");
  }

  @Test
  void aGenuineSignatureVerifies() {
    String sig = signature.sign(7L, UPLOAD, EXPIRY);

    assertThat(signature.matches(7L, UPLOAD, EXPIRY, sig)).isTrue();
  }

  @Test
  void aSignatureDoesNotCarryOverToAnotherJob() {
    String sig = signature.sign(7L, UPLOAD, EXPIRY);

    assertThat(signature.matches(8L, UPLOAD, EXPIRY, sig)).isFalse();
  }

  @Test
  void readAccessIsNotWriteAccess() {
    String download = signature.sign(7L, DOWNLOAD, EXPIRY);

    assertThat(signature.matches(7L, UPLOAD, EXPIRY, download)).isFalse();
    assertThat(signature.sign(7L, UPLOAD, EXPIRY)).isNotEqualTo(download);
  }

  @Test
  void extendingTheDeadlineInvalidatesTheSignature() {
    String sig = signature.sign(7L, UPLOAD, EXPIRY);

    assertThat(signature.matches(7L, UPLOAD, EXPIRY.plusSeconds(3600), sig)).isFalse();
  }

  @Test
  void aSignatureFromAnotherSecretIsWorthless() {
    StorageSignature other = new StorageSignature("a-different-secret-of-sufficient-length");

    assertThat(signature.matches(7L, UPLOAD, EXPIRY, other.sign(7L, UPLOAD, EXPIRY))).isFalse();
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"token-7", "not base64 at all!!", "AAAA"})
  void garbageIsRejectedRatherThanThrown(String candidate) {
    assertThat(signature.matches(7L, UPLOAD, EXPIRY, candidate)).isFalse();
  }

  /**
   * The storage side verifies URLs it did not issue, and a second replica or a restarted process
   * has to reach the same answer from the same secret.
   */
  @Test
  void anotherInstanceHoldingTheSameSecretVerifiesTheSameUrls() {
    StorageSignature replica = new StorageSignature("a-secret-that-is-long-enough-to-sign");

    assertThat(replica.matches(7L, UPLOAD, EXPIRY, signature.sign(7L, UPLOAD, EXPIRY))).isTrue();
  }

  @Test
  void theSignatureIsUrlSafeSoItSurvivesBeingPutInAQueryString() {
    String sig = signature.sign(7L, UPLOAD, EXPIRY);

    assertThat(sig).matches("[A-Za-z0-9_-]+");
  }
}
