package com.yourco.compute.orchestrator.service;

import com.yourco.compute.adapters.core.ProviderClient;
import com.yourco.compute.adapters.core.ProvisionResult;
import com.yourco.compute.billing.ledger.LedgerService;
import com.yourco.compute.domain.model.OutboxEvent;
import com.yourco.compute.domain.repo.OutboxEventRepository;
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
  private final JobRepository jobs;
  private final LedgerService ledger;
  private final Map<String, ProviderClient> providers;
  private final SelectionPolicy policy = new BalancedPolicy();
  private final OutboxEventRepository outbox;
  private final QuoteService quotes;

  public JobOrchestrator(JobRepository jobs, LedgerService ledger, List<ProviderClient> providerClients,
                         OutboxEventRepository outbox, QuoteService quotes){
    this.jobs = jobs;
    this.ledger = ledger;
    this.outbox = outbox;
    this.quotes = quotes;
    this.providers = providerClients.stream()
        .collect(Collectors.toMap(pc -> pc.getClass().getSimpleName(), pc -> pc));
  }

  @Transactional
  public Job submit(Job job){
    job.setStatus(JobStatus.QUEUED);
    Job saved = jobs.save(job);

    OutboxEvent ev = new OutboxEvent();
    ev.setEventType("JobSubmitted");
    ev.setAggregateType("Job");
    ev.setAggregateId(saved.getId());
    ev.setPayload("{\"jobId\":" + saved.getId() + "}");
    outbox.save(ev);

    List<QuoteService.Quote> qs = quotes.getQuotes("us-east-1", "A100-80G");
    List<SelectionPolicy.Quote> policyQuotes = qs.stream()
        .map(q -> new SelectionPolicy.Quote(q.provider(), q.onDemandPerHour(), q.latencyMs(), q.reliability()))
        .toList();
    SelectionPolicy.Quote choice = policy.pick(policyQuotes);

    BigDecimal hold = BigDecimal.valueOf(choice.estCost() * 1.2);
    ledger.hold(UUID.randomUUID(), saved.getUserId(), hold, saved.getId());

    ProviderClient client = providers.get(choice.provider());
    if (client == null) {
      saved.setStatus(JobStatus.FAILED);
      jobs.save(saved);
      throw new IllegalStateException("Provider not found: " + choice.provider());
    }

    ProvisionResult pr = client.provision(saved);
    client.start(pr.instanceId());

    saved.setStatus(JobStatus.RUNNING);
    saved.setStartedAt(Instant.now());

    OutboxEvent ev2 = new OutboxEvent();
    ev2.setEventType("JobStarted");
    ev2.setAggregateType("Job");
    ev2.setAggregateId(saved.getId());
    ev2.setPayload("{\"jobId\":" + saved.getId() + "}");
    outbox.save(ev2);

    return jobs.save(saved);
  }

  @Transactional(readOnly = true)
  public Job get(long id){
    return jobs.findById(id).orElseThrow();
  }
}
