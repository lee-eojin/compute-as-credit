package com.yourco.compute.agent;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Client for the job API. The caller is identified by the bearer token, so the job is always
 * billed to the token's subject.
 */
public class AgentClient {
  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
  private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);

  private final RestTemplate rest;
  private final String baseUrl;
  private final Supplier<String> accessToken;

  public AgentClient(String baseUrl, Supplier<String> accessToken) {
    this(baseUrl, accessToken, defaultRestTemplate());
  }

  public AgentClient(String baseUrl, Supplier<String> accessToken, RestTemplate rest) {
    this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl");
    this.accessToken = Objects.requireNonNull(accessToken, "accessToken");
    this.rest = Objects.requireNonNull(rest, "rest");
  }

  /**
   * Submits under a freshly generated idempotency key. Retrying a failed submit through this
   * overload creates a second job, so retries should go through
   * {@link #submitJob(JobRequest, String)} with the key of the original attempt.
   */
  public JobResponse submitJob(JobRequest request) {
    return submitJob(request, UUID.randomUUID().toString());
  }

  public JobResponse submitJob(JobRequest request, String idempotencyKey) {
    HttpHeaders headers = authHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Idempotency-Key", Objects.requireNonNull(idempotencyKey, "idempotencyKey"));

    ResponseEntity<JobResponse> response = rest.exchange(
        baseUrl + "/v1/jobs", HttpMethod.POST, new HttpEntity<>(request, headers), JobResponse.class);
    return body(response, "POST /v1/jobs");
  }

  public JobStatus getJob(long jobId) {
    ResponseEntity<JobStatus> response = rest.exchange(
        baseUrl + "/v1/jobs/" + jobId, HttpMethod.GET, new HttpEntity<>(authHeaders()), JobStatus.class);
    return body(response, "GET /v1/jobs/" + jobId);
  }

  public static RestTemplate defaultRestTemplate() {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout((int) CONNECT_TIMEOUT.toMillis());
    factory.setReadTimeout((int) READ_TIMEOUT.toMillis());
    return new RestTemplate(factory);
  }

  private HttpHeaders authHeaders() {
    String token = accessToken.get();
    if (token == null || token.isBlank()) {
      throw new IllegalStateException("Access token supplier returned no token");
    }
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    headers.setAccept(List.of(MediaType.APPLICATION_JSON));
    return headers;
  }

  private static <T> T body(ResponseEntity<T> response, String call) {
    T value = response.getBody();
    if (value == null) {
      throw new IllegalStateException(call + " answered " + response.getStatusCode() + " with an empty body");
    }
    return value;
  }

  /** {@code agentSpec} and {@code resourceHint} must be JSON objects; {@code maxBudget} may be null for no cap. */
  public record JobRequest(String agentSpec, String resourceHint, Double maxBudget) {}
  public record JobResponse(Long jobId, String status) {}
  public record JobStatus(Long jobId, String status, Long providerId) {}
}
