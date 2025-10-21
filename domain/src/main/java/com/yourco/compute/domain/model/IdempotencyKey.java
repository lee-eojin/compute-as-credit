package com.yourco.compute.domain.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="idempotency_keys")
public class IdempotencyKey {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @Column(unique=true, name="idem_key") private String key;
  private String scope; // e.g., JOB_SUBMIT
  private Long jobId;
  private Instant createdAt = Instant.now();

  public IdempotencyKey() {}
  public IdempotencyKey(String key, String scope, Long jobId){ this.key=key; this.scope=scope; this.jobId=jobId; }

  public String getKey(){return key;}
  public String getScope(){return scope;}
  public Long getJobId(){return jobId;}
}
