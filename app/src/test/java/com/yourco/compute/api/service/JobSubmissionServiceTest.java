package com.yourco.compute.api.service;

import com.yourco.compute.domain.model.IdempotencyKey;
import com.yourco.compute.domain.model.Job;
import com.yourco.compute.domain.model.JobStatus;
import com.yourco.compute.domain.model.Provider;
import com.yourco.compute.domain.repo.IdempotencyKeyRepository;
import com.yourco.compute.domain.repo.JobRepository;
import com.yourco.compute.domain.repo.OutboxEventRepository;
import com.yourco.compute.domain.repo.ProviderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs without a surrounding test transaction on purpose: the point of these cases is what survives
 * a commit and what does not.
 */
@SpringBootTest
@ActiveProfiles("test")
class JobSubmissionServiceTest {
  private static final long USER = 42L;

  @Autowired JobSubmissionService submissions;
  @Autowired JobRepository jobs;
  @Autowired IdempotencyKeyRepository keys;
  @Autowired OutboxEventRepository outbox;
  @Autowired ProviderRepository providers;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  void reset() {
    keys.deleteAll();
    jobs.deleteAll();
    outbox.deleteAll();
    providers.deleteAll();
    jdbc.update("DELETE FROM ledger_postings");
    jdbc.update("DELETE FROM ledger_entries");
    jdbc.update("DELETE FROM ledger_accounts");
    providers.save(registered("FakeProviderClient"));
  }

  @Test
  void aSubmitAndItsKeyLandTogether() {
    Job saved = submissions.submit(job(), "key-1");

    assertThat(saved.getStatus()).isEqualTo(JobStatus.RUNNING);
    assertThat(jobs.count()).isEqualTo(1);
    assertThat(keys.findByKeyAndScopeAndUserId("key-1", JobSubmissionService.SUBMIT_SCOPE, USER))
        .get()
        .extracting(IdempotencyKey::getJobId)
        .isEqualTo(saved.getId());
    assertThat(ledgerEntries()).isEqualTo(1);
  }

  @Test
  void aReplayedKeyReturnsTheFirstJobWithoutASecondHold() {
    Job first = submissions.submit(job(), "key-1");
    Job replay = submissions.submit(job(), "key-1");

    assertThat(replay.getId()).isEqualTo(first.getId());
    assertThat(jobs.count()).isEqualTo(1);
    assertThat(ledgerEntries()).isEqualTo(1);
  }

  @Test
  void thesameKeyFromAnotherCallerIsItsOwnSubmission() {
    submissions.submit(job(), "key-1");

    Job other = new Job();
    other.setUserId(99L);
    other.setAgentSpec("{}");
    other.setStatus(JobStatus.SUBMITTED);
    submissions.submit(other, "key-1");

    assertThat(jobs.count()).isEqualTo(2);
    assertThat(ledgerEntries()).isEqualTo(2);
  }

  @Test
  void aSubmitWithoutAKeyIsStillAccepted() {
    Job saved = submissions.submit(job(), null);

    assertThat(saved.getStatus()).isEqualTo(JobStatus.RUNNING);
    assertThat(keys.count()).isZero();
  }

  /**
   * The concurrency guard from the issue. A second writer that got past the lookup has to lose
   * here, otherwise both would reach the ledger and charge the caller twice.
   */
  @Test
  void theUniqueIndexRejectsASecondKeyForTheSameCallerAndScope() {
    keys.saveAndFlush(new IdempotencyKey("key-1", JobSubmissionService.SUBMIT_SCOPE, USER, 1L));

    assertThatThrownBy(() ->
        keys.saveAndFlush(new IdempotencyKey("key-1", JobSubmissionService.SUBMIT_SCOPE, USER, 2L)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void aBudgetRejectionLeavesNeitherAJobNorAKeyBehind() {
    Job tooExpensive = job();
    tooExpensive.setMaxBudget(0.01);

    assertThatThrownBy(() -> submissions.submit(tooExpensive, "key-1"))
        .isInstanceOf(RuntimeException.class);

    assertThat(jobs.count()).isZero();
    assertThat(keys.count()).isZero();
    assertThat(ledgerEntries()).isZero();
  }

  private Integer ledgerEntries() {
    return jdbc.queryForObject("SELECT COUNT(*) FROM ledger_entries", Integer.class);
  }

  private static Job job() {
    Job job = new Job();
    job.setUserId(USER);
    job.setAgentSpec("{}");
    job.setStatus(JobStatus.SUBMITTED);
    return job;
  }

  private static Provider registered(String name) {
    Provider provider = new Provider();
    provider.setName(name);
    provider.setRegion("us-east-1");
    provider.setStatus("ACTIVE");
    return provider;
  }
}
