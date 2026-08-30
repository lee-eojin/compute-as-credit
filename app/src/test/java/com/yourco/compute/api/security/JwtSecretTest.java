package com.yourco.compute.api.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtSecretTest {

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  void anAbsentSecretFailsStartupInsteadOfAnsweringUnauthorizedForever(String secret) {
    assertThatThrownBy(() -> JwtSecret.toKey(secret))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("JWT_SECRET");
  }

  @Test
  void theOldDevelopmentSecretIsTooShortForHs256() {
    assertThatThrownBy(() -> JwtSecret.toKey("dev-secret"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("10 bytes")
        .hasMessageContaining("at least 32");
  }

  @Test
  void aSecretOneByteUnderTheAlgorithmMinimumIsRejected() {
    assertThatThrownBy(() -> JwtSecret.toKey("x".repeat(JwtSecret.MIN_BYTES - 1)))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void lengthIsCountedInBytesRatherThanCharacters() {
    // 16 characters, but 48 bytes once encoded, so it clears the bar a character count would miss.
    String multiByte = "가".repeat(16);

    assertThat(JwtSecret.toKey(multiByte).getEncoded()).hasSize(48);
  }

  @Test
  void aSecretAtTheMinimumProducesAnHmacKey() {
    var key = JwtSecret.toKey("x".repeat(JwtSecret.MIN_BYTES));

    assertThat(key.getAlgorithm()).isEqualTo("HmacSHA256");
    assertThat(key.getEncoded()).hasSize(JwtSecret.MIN_BYTES);
  }
}
