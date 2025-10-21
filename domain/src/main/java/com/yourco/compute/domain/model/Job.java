package com.yourco.compute.domain.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "jobs")
public class Job {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  private Long userId;
  private Long providerId;

  @Enumerated(EnumType.STRING)
  private JobStatus status = JobStatus.SUBMITTED;

  @Lob @Column(columnDefinition = "json")
  private String agentSpec; // JSON

  @Lob @Column(columnDefinition = "json")
  private String resourceHint; // JSON

  private Double maxBudget;
  private Instant createdAt = Instant.now();
  private Instant startedAt;
  private Instant endedAt;

  public Long getId() { return id; }
  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public Long getProviderId() { return providerId; }
  public void setProviderId(Long providerId) { this.providerId = providerId; }
  public JobStatus getStatus() { return status; }
  public void setStatus(JobStatus status) { this.status = status; }
  public String getAgentSpec() { return agentSpec; }
  public void setAgentSpec(String agentSpec) { this.agentSpec = agentSpec; }
  public String getResourceHint() { return resourceHint; }
  public void setResourceHint(String resourceHint) { this.resourceHint = resourceHint; }
  public Double getMaxBudget() { return maxBudget; }
  public void setMaxBudget(Double maxBudget) { this.maxBudget = maxBudget; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getStartedAt() { return startedAt; }
  public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
  public Instant getEndedAt() { return endedAt; }
  public void setEndedAt(Instant endedAt) { this.endedAt = endedAt; }
}
