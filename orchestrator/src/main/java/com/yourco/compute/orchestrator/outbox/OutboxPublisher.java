package com.yourco.compute.orchestrator.outbox;

import com.yourco.compute.domain.model.OutboxEvent;
import com.yourco.compute.domain.repo.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class OutboxPublisher {
  private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
  private final OutboxEventRepository repo;
  private final RabbitTemplate rabbit;

  public OutboxPublisher(OutboxEventRepository repo, RabbitTemplate rabbit){ this.repo=repo; this.rabbit=rabbit; }

  @Scheduled(fixedDelay = 2000)
  @Transactional
  public void publish() {
    var events = repo.findTop50ByProcessedAtIsNullOrderByCreatedAtAsc();
    for (OutboxEvent ev : events) {
      try {
        rabbit.convertAndSend("compute.events", "job."+ev.getEventType().toLowerCase(), ev.getPayload());
        ev.setProcessedAt(Instant.now());
        repo.save(ev);
      } catch (Exception e) {
        log.error("Failed to publish event {}: {}", ev.getId(), e.getMessage(), e);
      }
    }
  }
}
