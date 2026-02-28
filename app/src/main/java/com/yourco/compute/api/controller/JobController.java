package com.yourco.compute.api.controller;

import com.yourco.compute.api.dto.JobApiModels.*;
import com.yourco.compute.api.infra.IdempotencyService;
import com.yourco.compute.domain.model.Job;
import com.yourco.compute.domain.model.JobStatus;
import com.yourco.compute.orchestrator.service.JobOrchestrator;
import com.yourco.compute.orchestrator.storage.StorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/v1/jobs")
public class JobController {
  private final JobOrchestrator orchestrator;
  private final StorageService storage;
  private final IdempotencyService idem;

  public JobController(JobOrchestrator orchestrator, StorageService storage, IdempotencyService idem){
    this.orchestrator = orchestrator;
    this.storage = storage;
    this.idem = idem;
  }

  @PostMapping
  public ResponseEntity<SubmitRes> submit(@RequestHeader(name="Idempotency-Key", required=false) String idemKey,
                                           @RequestBody @Validated SubmitReq req){
    if (idemKey != null) {
      Optional<Long> existing = idem.findJob(idemKey, "JOB_SUBMIT");
      if (existing.isPresent()) {
        Job j = orchestrator.get(existing.get());
        return ResponseEntity.ok(new SubmitRes(j.getId(), j.getStatus().name()));
      }
    }

    Job job = new Job();
    job.setUserId(req.userId());
    job.setAgentSpec(req.agentSpec());
    job.setResourceHint(req.resourceHint());
    job.setMaxBudget(req.maxBudget());
    job.setStatus(JobStatus.SUBMITTED);

    Job saved = orchestrator.submit(job);

    if (idemKey != null) {
      idem.remember(idemKey, "JOB_SUBMIT", saved.getId());
    }

    return ResponseEntity.ok(new SubmitRes(saved.getId(), saved.getStatus().name()));
  }

  @GetMapping("/{id}")
  public ResponseEntity<JobRes> get(@PathVariable long id){
    Job j = orchestrator.get(id);
    return ResponseEntity.ok(new JobRes(j.getId(), j.getStatus().name(), j.getProviderId()));
  }

  @PostMapping("/{id}/io")
  public ResponseEntity<Object> allocateIO(@PathVariable long id){
    StorageService.IOUrls urls = storage.allocateForJob(id);
    return ResponseEntity.ok(urls);
  }
}
