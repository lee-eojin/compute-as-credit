package com.yourco.compute.api.service;

import com.yourco.compute.api.infra.IdempotencyService;
import com.yourco.compute.domain.model.Job;
import com.yourco.compute.orchestrator.service.JobOrchestrator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Runs a submit and the record of its idempotency key inside one transaction.
 *
 * <p>With two transactions a crash between them leaves a job and a ledger hold behind with no key,
 * so the documented retry creates a second one. Joined, either both land or neither does, and a
 * concurrent request carrying the same key loses on the unique index and rolls its own hold back.
 */
@Service
public class JobSubmissionService {
  public static final String SUBMIT_SCOPE = "JOB_SUBMIT";

  private final JobOrchestrator orchestrator;
  private final IdempotencyService idem;

  public JobSubmissionService(JobOrchestrator orchestrator, IdempotencyService idem) {
    this.orchestrator = orchestrator;
    this.idem = idem;
  }

  @Transactional
  public Job submit(Job job, String idempotencyKey) {
    long userId = job.getUserId();
    if (idempotencyKey == null) {
      return orchestrator.submit(job);
    }

    Optional<Long> replayed = idem.findJob(idempotencyKey, SUBMIT_SCOPE, userId);
    if (replayed.isPresent()) {
      return orchestrator.getForUser(replayed.get(), userId);
    }

    Job saved = orchestrator.submit(job);
    idem.remember(idempotencyKey, SUBMIT_SCOPE, userId, saved.getId());
    return saved;
  }
}
