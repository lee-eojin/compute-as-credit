package com.yourco.compute.orchestrator.quotes;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.*;

@Service
public class QuoteService {
  public record Quote(String provider, String region, String gpuType, double onDemandPerHour, double latencyMs, double reliability){}
  private final Cache<String, List<Quote>> cache = Caffeine.newBuilder().expireAfterWrite(Duration.ofSeconds(45)).maximumSize(1000).build();

  public List<Quote> getQuotes(String region, String gpuType){
    String key = region+"|"+gpuType;
    return cache.get(key, k -> fetchFromProviders(region, gpuType));
  }

  private List<Quote> fetchFromProviders(String region, String gpuType){
    return List.of(
      new Quote("FakeProviderClient", region, gpuType, 0.50, 800, 0.98),
      new Quote("RunPodClient",       region, gpuType, 0.62, 650, 0.97)
    );
  }
}
