package com.yourco.compute.domain.model;

import jakarta.persistence.*;

@Entity @Table(name = "providers")
public class Provider {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  private String name;
  private String region;
  private String status;

  @Lob @Column(columnDefinition = "json")
  private String pricingJson;

  @Lob @Column(columnDefinition = "json")
  private String metricsJson;

  public Long getId(){return id;}
  public String getName(){return name;}
  public void setName(String n){this.name=n;}
  public String getRegion(){return region;}
  public void setRegion(String r){this.region=r;}
  public String getStatus(){return status;}
  public void setStatus(String s){this.status=s;}
  public String getPricingJson(){return pricingJson;}
  public void setPricingJson(String p){this.pricingJson=p;}
  public String getMetricsJson(){return metricsJson;}
  public void setMetricsJson(String m){this.metricsJson=m;}
}
