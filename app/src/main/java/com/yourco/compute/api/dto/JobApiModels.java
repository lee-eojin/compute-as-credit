package com.yourco.compute.api.dto;

import com.yourco.compute.api.validation.ValidJson;
import jakarta.validation.constraints.*;

public class JobApiModels {
  /** The billed user comes from the bearer token, never from the body. */
  public record SubmitReq(
    @NotNull @ValidJson String agentSpec,
    @ValidJson String resourceHint,
    // Bounded by the max_budget column, which is DECIMAL(18,6). Without an upper bound a value such
    // as 1e309 arrives as Double.POSITIVE_INFINITY, passes @Positive, and blows up in BigDecimal.
    @Positive @DecimalMax("999999999999.999999") Double maxBudget
  ){}
  public record SubmitRes(Long jobId, String status){}
  public record JobRes(Long jobId, String status, Long providerId){}
}
