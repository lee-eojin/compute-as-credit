package com.yourco.compute.orchestrator.selector;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BalancedPolicyTest {
  private final BalancedPolicy policy = new BalancedPolicy();

  @Test
  void millisecondsDoNotOutweighDollars() {
    // The shipped quotes. Scoring latency on its raw millisecond scale used to pick the dearer
    // provider purely because it answered 150ms sooner.
    SelectionPolicy.Quote cheap = new SelectionPolicy.Quote("FakeProviderClient", 0.50, 800, 0.98);
    SelectionPolicy.Quote dear = new SelectionPolicy.Quote("RunPodClient", 0.62, 650, 0.97);

    assertThat(policy.pick(List.of(cheap, dear))).isEqualTo(cheap);
    assertThat(policy.pick(List.of(dear, cheap))).isEqualTo(cheap);
  }

  @Test
  void aLargeEnoughPriceGapOutweighsBetterLatencyAndReliability() {
    SelectionPolicy.Quote cheap = new SelectionPolicy.Quote("cheap", 0.10, 5000, 0.80);
    SelectionPolicy.Quote dear = new SelectionPolicy.Quote("dear", 9.00, 10, 1.00);

    assertThat(policy.pick(List.of(cheap, dear))).isEqualTo(cheap);
  }

  @Test
  void latencyStillDecidesWhenPriceAndReliabilityMatch() {
    SelectionPolicy.Quote slow = new SelectionPolicy.Quote("slow", 1.00, 900, 0.99);
    SelectionPolicy.Quote fast = new SelectionPolicy.Quote("fast", 1.00, 100, 0.99);

    assertThat(policy.pick(List.of(slow, fast))).isEqualTo(fast);
  }

  @Test
  void reliabilityBreaksAnOtherwiseIdenticalTie() {
    SelectionPolicy.Quote flaky = new SelectionPolicy.Quote("flaky", 1.00, 500, 0.90);
    SelectionPolicy.Quote solid = new SelectionPolicy.Quote("solid", 1.00, 500, 0.99);

    assertThat(policy.pick(List.of(flaky, solid))).isEqualTo(solid);
  }

  @Test
  void aFreeInstantQuoteDoesNotDivideByZero() {
    SelectionPolicy.Quote free = new SelectionPolicy.Quote("free", 0.0, 0.0, 1.0);

    assertThat(policy.pick(List.of(free))).isEqualTo(free);
  }
}
