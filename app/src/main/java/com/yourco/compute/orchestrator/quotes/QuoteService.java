package com.yourco.compute.orchestrator.quotes;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.yourco.compute.domain.model.ResourceHint;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.*;

@Service
public class QuoteService {
  public record Quote(String provider, String region, String gpuType, double onDemandPerHour, double latencyMs, double reliability){}
  private final Cache<String, List<Quote>> cache = Caffeine.newBuilder().expireAfterWrite(Duration.ofSeconds(45)).maximumSize(1000).build();

  /**
   * Takes a parsed hint rather than two strings so the cache key can only ever come from a value
   * the platform supports. A caller-supplied region would let one tenant evict everyone's entries.
   */
  public List<Quote> getQuotes(ResourceHint hint){
    return cache.get(hint.quoteKey(), k -> fetchFromProviders(hint.region(), hint.gpuType()));
  }

  private List<Quote> fetchFromProviders(String region, String gpuType){
    return List.of(
      new Quote("FakeProviderClient", region, gpuType, 0.50, 800, 0.98),
      new Quote("RunPodClient",       region, gpuType, 0.62, 650, 0.97)
    );
  }
}
