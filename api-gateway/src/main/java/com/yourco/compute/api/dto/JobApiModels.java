package com.yourco.compute.api.dto;

import jakarta.validation.constraints.*;

public class JobApiModels {
  public record SubmitReq(
    @NotNull Long userId,
    String agentSpec,
    String resourceHint,
    @Positive Double maxBudget
  ){}
  public record SubmitRes(Long jobId, String status){}
  public record JobRes(Long jobId, String status, Long providerId){}
}
