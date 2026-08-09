package com.yourco.compute.orchestrator.selector;

import java.util.List;

public interface SelectionPolicy {
  /** {@code ratePerHour} is a rate, not a job total: nothing here knows how long a job runs. */
  record Quote(String provider, double ratePerHour, double latencyMs, double reliability){ }
  Quote pick(List<Quote> quotes);
}
