package com.yourco.compute.domain.model;

import jakarta.persistence.*;
import java.time.Instant;

/** The constraint is what makes a concurrent replay lose, so it is declared here as well as in V3. */
@Entity
@Table(name="idempotency_keys", uniqueConstraints =
    @UniqueConstraint(name = "uk_idempotency_scope_user", columnNames = {"idem_key", "scope", "user_id"}))
public class IdempotencyKey {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @Column(name="idem_key", length=64, nullable=false) private String key;
  @Column(length=64, nullable=false) private String scope; // e.g., JOB_SUBMIT
  private Long userId;
  private Long jobId;
  private Instant createdAt = Instant.now();

  public IdempotencyKey() {}
  public IdempotencyKey(String key, String scope, Long userId, Long jobId){
    this.key=key; this.scope=scope; this.userId=userId; this.jobId=jobId;
  }

  public String getKey(){return key;}
  public String getScope(){return scope;}
  public Long getUserId(){return userId;}
  public Long getJobId(){return jobId;}
  public Instant getCreatedAt(){return createdAt;}
}
