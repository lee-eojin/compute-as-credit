package com.yourco.compute.billing.ledger;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "ledger_accounts")
class LedgerAccount {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  private Long userId;
  @Enumerated(EnumType.STRING)
  private Type type;
  private String currency = "USD";

  public enum Type { ASSET, LIABILITY, REVENUE, EXPENSE }

  public Long getId() { return id; }
  public Long getUserId() { return userId; }
  public void setUserId(Long u) { this.userId = u; }
  public Type getType() { return type; }
  public void setType(Type t) { this.type = t; }
  public String getCurrency() { return currency; }
  public void setCurrency(String c) { this.currency = c; }
}

@Entity
@Table(name = "ledger_entries")
class LedgerEntry {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  private Long jobId;
  @Enumerated(EnumType.STRING)
  private Kind kind;
  private Instant createdAt = Instant.now();
  @Column(unique = true)
  private String idempotencyKey;

  public enum Kind { HOLD, DEBIT, REFUND, CHARGE }

  public Long getId() { return id; }
  public Long getJobId() { return jobId; }
  public void setJobId(Long j) { this.jobId = j; }
  public Kind getKind() { return kind; }
  public void setKind(Kind k) { this.kind = k; }
  public Instant getCreatedAt() { return createdAt; }
  public String getIdempotencyKey() { return idempotencyKey; }
  public void setIdempotencyKey(String k) { this.idempotencyKey = k; }
}

@Entity
@Table(name = "ledger_postings")
class LedgerPosting {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  private Long entryId;
  private Long accountId;
  @Enumerated(EnumType.STRING)
  private Side side;
  private BigDecimal amount;

  public enum Side { DEBIT, CREDIT }

  public Long getId() { return id; }
  public Long getEntryId() { return entryId; }
  public void setEntryId(Long e) { this.entryId = e; }
  public Long getAccountId() { return accountId; }
  public void setAccountId(Long a) { this.accountId = a; }
  public Side getSide() { return side; }
  public void setSide(Side s) { this.side = s; }
  public BigDecimal getAmount() { return amount; }
  public void setAmount(BigDecimal a) { this.amount = a; }
}
