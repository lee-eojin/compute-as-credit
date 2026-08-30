package com.yourco.compute.api.service;

import com.yourco.compute.api.infra.IdempotencyService;
import com.yourco.compute.domain.model.Job;
import com.yourco.compute.domain.model.JobStatus;
import com.yourco.compute.domain.model.Provider;
import com.yourco.compute.domain.repo.JobRepository;
import com.yourco.compute.domain.repo.ProviderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.willThrow;

/**
 * The failure the two-transaction version could not survive: the job commits, the key write does
 * not, and the documented retry bills the caller a second time. One transaction means the job and
 * its hold go back with the key.
 */
@SpringBootTest
@ActiveProfiles("test")
class JobSubmissionRollbackTest {

  @Autowired JobSubmissionService submissions;
  @Autowired JobRepository jobs;
  @Autowired ProviderRepository providers;
  @Autowired JdbcTemplate jdbc;

  @MockBean IdempotencyService idem;

  @BeforeEach
  void reset() {
    jobs.deleteAll();
    providers.deleteAll();
    jdbc.update("DELETE FROM ledger_postings");
    jdbc.update("DELETE FROM ledger_entries");
    jdbc.update("DELETE FROM ledger_accounts");

    Provider provider = new Provider();
    provider.setName("FakeProviderClient");
    provider.setRegion("us-east-1");
    provider.setStatus("ACTIVE");
    providers.save(provider);
  }

  @Test
  void aFailedKeyWriteTakesTheJobAndTheHoldWithIt() {
    willThrow(new DataIntegrityViolationException("uk_idempotency_scope_user"))
        .given(idem).remember(anyString(), anyString(), anyLong(), any());

    Job job = new Job();
    job.setUserId(42L);
    job.setAgentSpec("{}");
    job.setStatus(JobStatus.SUBMITTED);

    assertThatThrownBy(() -> submissions.submit(job, "key-1"))
        .isInstanceOf(DataIntegrityViolationException.class);

    assertThat(jobs.count()).isZero();
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ledger_entries", Integer.class)).isZero();
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ledger_postings", Integer.class)).isZero();
  }
}
