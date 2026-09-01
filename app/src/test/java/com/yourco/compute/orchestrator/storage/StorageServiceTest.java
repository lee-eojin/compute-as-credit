package com.yourco.compute.orchestrator.storage;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static com.yourco.compute.orchestrator.storage.StorageSignature.Operation.DOWNLOAD;
import static com.yourco.compute.orchestrator.storage.StorageSignature.Operation.UPLOAD;
import static org.assertj.core.api.Assertions.assertThat;

class StorageServiceTest {
  private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

  private final StorageSignature signature = new StorageSignature("a-secret-that-is-long-enough-to-sign");
  private final StorageService storage = new StorageService(
      signature, "https://storage.internal", Duration.ofHours(1), Clock.fixed(NOW, ZoneOffset.UTC));

  @Test
  void theTokenIsNoLongerDerivableFromTheJobId() {
    StorageService.IOUrls urls = storage.allocateForJob(7L);

    assertThat(urls.uploadUrl()).doesNotContain("token-7");
    assertThat(urls.downloadUrl()).doesNotContain("token-7");
  }

  @Test
  void bothUrlsCarryASignatureTheStorageSideCanCheck() {
    StorageService.IOUrls urls = storage.allocateForJob(7L);

    assertThat(signature.matches(7L, UPLOAD, urls.expiresAt(), signatureIn(urls.uploadUrl()))).isTrue();
    assertThat(signature.matches(7L, DOWNLOAD, urls.expiresAt(), signatureIn(urls.downloadUrl()))).isTrue();
  }

  @Test
  void theUploadCapabilityIsNotAlsoTheDownloadCapability() {
    StorageService.IOUrls urls = storage.allocateForJob(7L);

    assertThat(signatureIn(urls.uploadUrl())).isNotEqualTo(signatureIn(urls.downloadUrl()));
  }

  @Test
  void oneJobsUrlsAreNoUseForAnother() {
    String sevens = signatureIn(storage.allocateForJob(7L).uploadUrl());

    assertThat(signature.matches(8L, UPLOAD, NOW.plus(Duration.ofHours(1)), sevens)).isFalse();
  }

  @Test
  void theDeadlineFollowsTheConfiguredTtlAndIsRepeatedInTheUrl() {
    StorageService.IOUrls urls = storage.allocateForJob(7L);

    assertThat(urls.expiresAt()).isEqualTo(NOW.plus(Duration.ofHours(1)));
    assertThat(urls.uploadUrl()).contains("expires=" + urls.expiresAt().getEpochSecond());
  }

  @Test
  void aShorterTtlProducesAnEarlierDeadline() {
    StorageService brief = new StorageService(
        signature, "https://storage.internal", Duration.ofMinutes(5), Clock.fixed(NOW, ZoneOffset.UTC));

    assertThat(brief.allocateForJob(7L).expiresAt()).isEqualTo(NOW.plusSeconds(300));
  }

  @Test
  void theObjectUrisStillPointAtTheJobsOwnPrefixes() {
    StorageService.IOUrls urls = storage.allocateForJob(7L);

    assertThat(urls.inputUri()).isEqualTo("s3://tenant/7/input/");
    assertThat(urls.outputUri()).isEqualTo("s3://tenant/7/output/");
  }

  private static String signatureIn(String url) {
    return url.substring(url.indexOf("&sig=") + "&sig=".length());
  }
}
