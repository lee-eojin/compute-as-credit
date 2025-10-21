package com.yourco.compute.orchestrator.selector;

import java.util.List;

public interface SelectionPolicy {
  record Quote(String provider, double estCost, double latencyMs, double reliability){ }
  Quote pick(List<Quote> quotes);
}
