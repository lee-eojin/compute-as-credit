package com.yourco.compute.billing.ledger;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "ledger_entries")
public class LedgerEntry {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  private Long jobId;
  @Enumerated(EnumType.STRING) private Kind kind;
  private Instant createdAt = Instant.now();
  @Column(unique = true) private String idempotencyKey;

  public enum Kind { HOLD, DEBIT, REFUND, CHARGE }

  public Long getId(){return id;}
  public Long getJobId(){return jobId;}
  public void setJobId(Long j){this.jobId=j;}
  public Kind getKind(){return kind;}
  public void setKind(Kind k){this.kind=k;}
  public Instant getCreatedAt(){return createdAt;}
  public String getIdempotencyKey(){return idempotencyKey;}
  public void setIdempotencyKey(String k){this.idempotencyKey=k;}
}
