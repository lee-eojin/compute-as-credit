package com.yourco.compute.billing.ledger;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity @Table(name = "ledger_postings")
public class LedgerPosting {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  private Long entryId;
  private Long accountId;
  @Enumerated(EnumType.STRING) private Side side;
  private BigDecimal amount;

  public enum Side { DEBIT, CREDIT }

  public Long getId(){return id;}
  public Long getEntryId(){return entryId;}
  public void setEntryId(Long e){this.entryId=e;}
  public Long getAccountId(){return accountId;}
  public void setAccountId(Long a){this.accountId=a;}
  public Side getSide(){return side;}
  public void setSide(Side s){this.side=s;}
  public BigDecimal getAmount(){return amount;}
  public void setAmount(BigDecimal a){this.amount=a;}
}
