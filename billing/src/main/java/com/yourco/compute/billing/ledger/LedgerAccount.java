package com.yourco.compute.billing.ledger;

import jakarta.persistence.*;

@Entity @Table(name = "ledger_accounts")
public class LedgerAccount {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  private Long userId;
  @Enumerated(EnumType.STRING) private Type type;
  private String currency = "USD";

  public enum Type { ASSET, LIABILITY, REVENUE, EXPENSE }

  public Long getId(){return id;}
  public Long getUserId(){return userId;}
  public void setUserId(Long u){this.userId=u;}
  public Type getType(){return type;}
  public void setType(Type t){this.type=t;}
  public String getCurrency(){return currency;}
  public void setCurrency(String c){this.currency=c;}
}
