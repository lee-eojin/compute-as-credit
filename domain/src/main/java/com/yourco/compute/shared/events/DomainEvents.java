package com.yourco.compute.shared.events;

public final class DomainEvents {
  public record JobSubmitted(long jobId, long userId) {}
  public record JobStarted(long jobId, long userId, long providerId) {}
  public record JobCompleted(long jobId, long userId, boolean success) {}
  public record UsageReported(long jobId, long providerId, double costEst) {}
  private DomainEvents() {}
}
