package com.yourco.compute.orchestrator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourco.compute.adapters.core.ProviderClient;
import com.yourco.compute.adapters.core.ProvisionResult;
import com.yourco.compute.billing.ledger.LedgerService;
import com.yourco.compute.domain.error.BudgetExceededException;
import com.yourco.compute.domain.error.JobNotFoundException;
import com.yourco.compute.domain.error.NoProviderAvailableException;
import com.yourco.compute.domain.model.OutboxEvent;
import com.yourco.compute.domain.model.Provider;
import com.yourco.compute.domain.model.ResourceHint;
import com.yourco.compute.domain.repo.OutboxEventRepository;
import com.yourco.compute.domain.repo.ProviderRepository;
import com.yourco.compute.orchestrator.quotes.QuoteService;
import com.yourco.compute.domain.model.Job;
import com.yourco.compute.domain.model.JobStatus;
import com.yourco.compute.domain.repo.JobRepository;
import com.yourco.compute.orchestrator.selector.BalancedPolicy;
import com.yourco.compute.orchestrator.selector.SelectionPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class JobOrchestrator {
  private static final BigDecimal HOLD_MARGIN = BigDecimal.valueOf(1.2);

  private final JobRepository jobs;
  private final LedgerService ledger;
  private final Map<String, ProviderClient> providers;
  private final SelectionPolicy policy;
  private final OutboxEventRepository outbox;
  private final QuoteService quotes;
  private final ProviderRepository providerRegistry;
  private final ObjectMapper mapper;

  public JobOrchestrator(JobRepository jobs, LedgerService ledger, List<ProviderClient> providerClients,
                         OutboxEventRepository outbox, QuoteService quotes,
                         ProviderRepository providerRegistry, ObjectMapper mapper,
                         SelectionPolicy policy){
    this.jobs = jobs;
    this.ledger = ledger;
    this.outbox = outbox;
    this.quotes = quotes;
    this.providerRegistry = providerRegistry;
    this.mapper = mapper;
    this.policy = policy;
    this.providers = providerClients.stream()
        .collect(Collectors.toMap(pc -> pc.getClass().getSimpleName(), pc -> pc));
  }

  @Transactional
  public Job submit(Job job){
    job.setStatus(JobStatus.QUEUED);
    Job saved = jobs.save(job);

    outbox.save(event("JobSubmitted", saved.getId()));

    ResourceHint hint = ResourceHint.parse(mapper, saved.getResourceHint());
    List<QuoteService.Quote> qs = quotes.getQuotes(hint.region(), hint.gpuType());
    // A quote for an adapter that is switched off would be picked and then fail at provision time.
    List<SelectionPolicy.Quote> policyQuotes = qs.stream()
        .filter(q -> providers.containsKey(q.provider()))
        .map(q -> new SelectionPolicy.Quote(q.provider(), q.onDemandPerHour(), q.latencyMs(), q.reliability()))
        .toList();
    if (policyQuotes.isEmpty()) {
      throw new NoProviderAvailableException(hint.region(), hint.gpuType());
    }
    SelectionPolicy.Quote choice = policy.pick(policyQuotes);

    BigDecimal hold = BigDecimal.valueOf(choice.ratePerHour()).multiply(HOLD_MARGIN);
    if (saved.getMaxBudget() != null) {
      BigDecimal cap = BigDecimal.valueOf(saved.getMaxBudget());
      if (hold.compareTo(cap) > 0) {
        throw new BudgetExceededException(hold, cap);
      }
    }

    ledger.hold(UUID.randomUUID(), saved.getUserId(), hold, saved.getId());

    ProviderClient client = providers.get(choice.provider());
    if (client == null) {
      saved.setStatus(JobStatus.FAILED);
      jobs.save(saved);
      throw new IllegalStateException("Provider not found: " + choice.provider());
    }
    Provider provider = providerRegistry.findByName(choice.provider())
        .orElseThrow(() -> new IllegalStateException("Provider is not registered: " + choice.provider()));

    ProvisionResult pr = client.provision(saved);
    client.start(pr.instanceId());

    saved.setProviderId(provider.getId());
    saved.setStatus(JobStatus.RUNNING);
    saved.setStartedAt(Instant.now());

    outbox.save(event("JobStarted", saved.getId()));

    return jobs.save(saved);
  }

  @Transactional(readOnly = true)
  public Job get(long id){
    return jobs.findById(id).orElseThrow(() -> new JobNotFoundException(id));
  }

  /** Another user's job is reported as missing rather than forbidden, so ids stay unguessable. */
  @Transactional(readOnly = true)
  public Job getForUser(long id, long userId){
    Job job = get(id);
    if (!job.isOwnedBy(userId)) {
      throw new JobNotFoundException(id);
    }
    return job;
  }

  private OutboxEvent event(String type, Long jobId){
    OutboxEvent ev = new OutboxEvent();
    ev.setEventType(type);
    ev.setAggregateType("Job");
    ev.setAggregateId(jobId);
    ev.setPayload("{\"jobId\":" + jobId + "}");
    return ev;
  }
}
