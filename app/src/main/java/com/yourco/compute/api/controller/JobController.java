package com.yourco.compute.api.controller;

import com.yourco.compute.api.dto.JobApiModels.*;
import com.yourco.compute.api.security.CallerId;
import com.yourco.compute.api.service.JobSubmissionService;
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

@RestController
@RequestMapping("/v1/jobs")
public class JobController {
  private final JobSubmissionService submissions;
  private final JobOrchestrator orchestrator;
  private final StorageService storage;

  public JobController(JobSubmissionService submissions, JobOrchestrator orchestrator, StorageService storage){
    this.submissions = submissions;
    this.orchestrator = orchestrator;
    this.storage = storage;
  }

  @PostMapping
  public ResponseEntity<SubmitRes> submit(@AuthenticationPrincipal Jwt jwt,
                                           @RequestHeader(name="Idempotency-Key", required=false)
                                           @Size(min=1, max=64) String idemKey,
                                           @RequestBody @Validated SubmitReq req){
    Job job = new Job();
    job.setUserId(CallerId.of(jwt));
    job.setAgentSpec(req.agentSpec());
    job.setResourceHint(req.resourceHint());
    job.setMaxBudget(req.maxBudget());
    job.setStatus(JobStatus.SUBMITTED);

    Job saved = submissions.submit(job, idemKey);
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
