package com.yourco.compute.agent;

import org.springframework.web.client.RestTemplate;

public class AgentClient {
  private final RestTemplate rest = new RestTemplate();
  private final String baseUrl;

  public AgentClient(String baseUrl) { this.baseUrl = baseUrl; }

  public JobResponse submitJob(JobRequest request) {
    return rest.postForObject(baseUrl + "/v1/jobs", request, JobResponse.class);
  }

  public JobStatus getJob(long jobId) {
    return rest.getForObject(baseUrl + "/v1/jobs/" + jobId, JobStatus.class);
  }

  public record JobRequest(long userId, String agentSpec, String resourceHint, double maxBudget) {}
  public record JobResponse(long jobId, String status) {}
  public record JobStatus(long jobId, String status, Long providerId) {}
}
