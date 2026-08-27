package com.yourco.compute.orchestrator.selector;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Cost first, then latency, then reliability. Each term is scaled against the worst candidate in
 * the same batch, because a raw millisecond figure would otherwise dwarf a dollar figure.
 */
@Component
public class BalancedPolicy implements SelectionPolicy {
  private static final double RATE_WEIGHT = 0.6;
  private static final double LATENCY_WEIGHT = 0.25;
  private static final double RELIABILITY_WEIGHT = 0.15;

  @Override
  public Quote pick(List<Quote> quotes) {
    double dearest = quotes.stream().mapToDouble(Quote::ratePerHour).max().orElse(0);
    double slowest = quotes.stream().mapToDouble(Quote::latencyMs).max().orElse(0);
    return quotes.stream()
        .min(Comparator.comparingDouble(q -> score(q, dearest, slowest)))
        .orElseThrow();
  }

  private double score(Quote q, double dearest, double slowest){
    return RATE_WEIGHT * relative(q.ratePerHour(), dearest)
        + LATENCY_WEIGHT * relative(q.latencyMs(), slowest)
        + RELIABILITY_WEIGHT * (1.0 - q.reliability());
  }

  private static double relative(double value, double worst){
    return worst <= 0 ? 0.0 : value / worst;
  }
}
