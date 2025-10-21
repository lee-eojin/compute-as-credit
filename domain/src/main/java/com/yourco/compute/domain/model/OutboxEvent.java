package com.yourco.compute.domain.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="outbox_events")
public class OutboxEvent {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  private String eventType;
  private String aggregateType;
  private Long aggregateId;
  private String correlationId;
  @Lob @Column(columnDefinition="json") private String payload;
  private Instant createdAt = Instant.now();
  private Instant processedAt;

  public Long getId(){return id;}
  public String getEventType(){return eventType;} public void setEventType(String v){this.eventType=v;}
  public String getAggregateType(){return aggregateType;} public void setAggregateType(String v){this.aggregateType=v;}
  public Long getAggregateId(){return aggregateId;} public void setAggregateId(Long v){this.aggregateId=v;}
  public String getCorrelationId(){return correlationId;} public void setCorrelationId(String v){this.correlationId=v;}
  public String getPayload(){return payload;} public void setPayload(String v){this.payload=v;}
  public Instant getCreatedAt(){return createdAt;}
  public Instant getProcessedAt(){return processedAt;} public void setProcessedAt(Instant v){this.processedAt=v;}
}
