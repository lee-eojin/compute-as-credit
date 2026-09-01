package com.yourco.compute.orchestrator.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class StorageService {
  public record IOUrls(String uploadUrl, String downloadUrl, String inputUri, String outputUri, Instant expiresAt){}

  private final StorageSignature signature;
  private final String baseUrl;
  private final Duration ttl;
  private final Clock clock;

  public StorageService(StorageSignature signature,
                        @Value("${storage.base-url:https://storage.internal}") String baseUrl,
                        @Value("${storage.url-ttl:PT1H}") Duration ttl,
                        Clock clock){
    this.signature = signature;
    this.baseUrl = baseUrl;
    this.ttl = ttl;
    this.clock = clock;
  }

  public IOUrls allocateForJob(long jobId){
    // Whole seconds, because that is what the signature covers and what the URL carries.
    Instant expiresAt = clock.instant().plus(ttl).truncatedTo(ChronoUnit.SECONDS);
    return new IOUrls(
        url(jobId, StorageSignature.Operation.UPLOAD, expiresAt),
        url(jobId, StorageSignature.Operation.DOWNLOAD, expiresAt),
        "s3://tenant/" + jobId + "/input/",
        "s3://tenant/" + jobId + "/output/",
        expiresAt);
  }

  private String url(long jobId, StorageSignature.Operation operation, Instant expiresAt){
    return baseUrl + "/" + operation.name().toLowerCase()
        + "?job=" + jobId
        + "&expires=" + expiresAt.getEpochSecond()
        + "&sig=" + signature.sign(jobId, operation, expiresAt);
  }
}
