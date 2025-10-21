package com.yourco.compute.orchestrator.selector;

import java.util.Comparator;
import java.util.List;

public class BalancedPolicy implements ProviderSelectionPolicy {
  @Override
  public Quote pick(List<Quote> qs) {
    return qs.stream().min(Comparator.comparingDouble(this::score)).orElseThrow();
  }
  private double score(Quote q){
    return 0.5*q.estCost() + 0.25*q.latencyMs()/100.0 + 0.2*(1.0 - q.reliability()) + 0.05*0.0;
  }
}
