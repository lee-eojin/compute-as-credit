package com.yourco.compute.orchestrator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourco.compute.adapters.core.ProviderClient;
import com.yourco.compute.adapters.fake.FakeProviderClient;
import com.yourco.compute.billing.ledger.LedgerService;
import com.yourco.compute.domain.error.BudgetExceededException;
import com.yourco.compute.domain.error.JobNotFoundException;
import com.yourco.compute.domain.model.Job;
import com.yourco.compute.domain.model.JobStatus;
import com.yourco.compute.domain.model.Provider;
import com.yourco.compute.domain.repo.JobRepository;
import com.yourco.compute.domain.repo.OutboxEventRepository;
import com.yourco.compute.domain.repo.ProviderRepository;
import com.yourco.compute.orchestrator.quotes.QuoteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class JobOrchestratorTest {
  private final JobRepository jobs = mock(JobRepository.class);
  private final LedgerService ledger = mock(LedgerService.class);
  private final OutboxEventRepository outbox = mock(OutboxEventRepository.class);
  private final QuoteService quotes = mock(QuoteService.class);
  private final ProviderRepository providerRegistry = mock(ProviderRepository.class);
  private final ProviderClient fake = new FakeProviderClient();
  private final Provider registeredProvider = provider(3L);

  private JobOrchestrator orchestrator;

  @BeforeEach
  void setUp() {
    orchestrator = new JobOrchestrator(jobs, ledger, List.of(fake), outbox, quotes,
        providerRegistry, new ObjectMapper());

    given(jobs.save(any())).willAnswer(inv -> inv.getArgument(0));
    given(quotes.getQuotes(anyString(), anyString())).willReturn(
        List.of(new QuoteService.Quote("FakeProviderClient", "any", "any", 0.50, 800, 0.98)));
    given(providerRegistry.findByName("FakeProviderClient")).willReturn(Optional.of(registeredProvider));
  }

  @Test
  void theResourceHintPicksTheRegionAndGpuTypeToQuote() {
    orchestrator.submit(job("{\"region\":\"eu-west-1\",\"gpuType\":\"H100-80G\"}", null));

    verify(quotes).getQuotes("eu-west-1", "H100-80G");
  }

  @Test
  void anAbsentResourceHintFallsBackToThePlatformDefaults() {
    orchestrator.submit(job(null, null));

    verify(quotes).getQuotes("us-east-1", "A100-80G");
  }

  @Test
  void theChosenProviderIsRecordedOnTheJob() {
    Job saved = orchestrator.submit(job("{}", null));

    assertThat(saved.getProviderId()).isEqualTo(3L);
    assertThat(saved.getStatus()).isEqualTo(JobStatus.RUNNING);
  }

  @Test
  void aHoldOverTheBudgetStopsTheJobBeforeAnythingIsCharged() {
    assertThatThrownBy(() -> orchestrator.submit(job("{}", 0.50)))
        .isInstanceOf(BudgetExceededException.class)
        .hasMessageContaining("0.60")
        .hasMessageContaining("0.5");

    verify(ledger, never()).hold(any(), anyLong(), any(), any());
  }

  @Test
  void aBudgetThatCoversTheHoldLetsTheJobThrough() {
    orchestrator.submit(job("{}", 1.0));

    verify(ledger).hold(any(UUID.class), eq(7L), eq(new BigDecimal("0.60")), any());
  }

  @Test
  void anotherUsersJobIsReportedAsMissing() {
    Job owned = job("{}", null);
    owned.setUserId(7L);
    given(jobs.findById(5L)).willReturn(Optional.of(owned));

    assertThatThrownBy(() -> orchestrator.getForUser(5L, 8L)).isInstanceOf(JobNotFoundException.class);
    assertThat(orchestrator.getForUser(5L, 7L)).isSameAs(owned);
  }

  @Test
  void anUnknownJobIdIsReportedAsMissing() {
    given(jobs.findById(404L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> orchestrator.get(404L)).isInstanceOf(JobNotFoundException.class);
  }

  private static Job job(String resourceHint, Double maxBudget) {
    Job job = new Job();
    job.setUserId(7L);
    job.setAgentSpec("{}");
    job.setResourceHint(resourceHint);
    job.setMaxBudget(maxBudget);
    return job;
  }

  private static Provider provider(long id) {
    Provider provider = mock(Provider.class);
    given(provider.getId()).willReturn(id);
    return provider;
  }
}
