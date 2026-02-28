package com.yourco.compute.orchestrator.reconcile;

import com.yourco.compute.domain.model.Job;
import com.yourco.compute.domain.model.JobStatus;
import com.yourco.compute.domain.repo.JobRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Instant;
import java.time.Duration;
import java.util.List;

@Component
public class Reconciler {
  private final JobRepository jobs;

  public Reconciler(JobRepository jobs){ this.jobs = jobs; }

  @Scheduled(fixedDelay = 30000)
  @Transactional
  public void sweep() {
    Instant cutoff = Instant.now().minus(Duration.ofMinutes(5));
    List<Job> stuck = jobs.findByStatus(JobStatus.QUEUED);
    for (Job j : stuck) {
      if (j.getCreatedAt() != null && j.getCreatedAt().isBefore(cutoff)) {
        j.setStatus(JobStatus.RUNNING);
        j.setStartedAt(Instant.now());
        jobs.save(j);
      }
    }
  }
}
