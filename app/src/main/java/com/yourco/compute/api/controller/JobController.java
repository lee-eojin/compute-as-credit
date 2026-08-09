package com.yourco.compute.api.controller;

import com.yourco.compute.api.dto.JobApiModels.*;
import com.yourco.compute.api.infra.IdempotencyService;
import com.yourco.compute.api.security.CallerId;
import com.yourco.compute.domain.model.Job;
import com.yourco.compute.domain.model.JobStatus;
import com.yourco.compute.orchestrator.service.JobOrchestrator;
import com.yourco.compute.orchestrator.storage.StorageService;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/v1/jobs")
public class JobController {
  private static final String SUBMIT_SCOPE = "JOB_SUBMIT";

  private final JobOrchestrator orchestrator;
  private final StorageService storage;
  private final IdempotencyService idem;

  public JobController(JobOrchestrator orchestrator, StorageService storage, IdempotencyService idem){
    this.orchestrator = orchestrator;
    this.storage = storage;
    this.idem = idem;
  }

  @PostMapping
  public ResponseEntity<SubmitRes> submit(@AuthenticationPrincipal Jwt jwt,
                                           @RequestHeader(name="Idempotency-Key", required=false)
                                           @Size(min=1, max=64) String idemKey,
                                           @RequestBody @Validated SubmitReq req){
    long userId = CallerId.of(jwt);

    if (idemKey != null) {
      Optional<Long> existing = idem.findJob(idemKey, SUBMIT_SCOPE, userId);
      if (existing.isPresent()) {
        Job j = orchestrator.getForUser(existing.get(), userId);
        return ResponseEntity.ok(new SubmitRes(j.getId(), j.getStatus().name()));
      }
    }

    Job job = new Job();
    job.setUserId(userId);
    job.setAgentSpec(req.agentSpec());
    job.setResourceHint(req.resourceHint());
    job.setMaxBudget(req.maxBudget());
    job.setStatus(JobStatus.SUBMITTED);

    Job saved = orchestrator.submit(job);

    if (idemKey != null) {
      idem.remember(idemKey, SUBMIT_SCOPE, userId, saved.getId());
    }

    return ResponseEntity.ok(new SubmitRes(saved.getId(), saved.getStatus().name()));
  }

  @GetMapping("/{id}")
  public ResponseEntity<JobRes> get(@AuthenticationPrincipal Jwt jwt, @PathVariable long id){
    Job j = orchestrator.getForUser(id, CallerId.of(jwt));
    return ResponseEntity.ok(new JobRes(j.getId(), j.getStatus().name(), j.getProviderId()));
  }

  @PostMapping("/{id}/io")
  public ResponseEntity<Object> allocateIO(@AuthenticationPrincipal Jwt jwt, @PathVariable long id){
    orchestrator.getForUser(id, CallerId.of(jwt));
    StorageService.IOUrls urls = storage.allocateForJob(id);
    return ResponseEntity.ok(urls);
  }
}
