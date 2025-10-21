package com.yourco.compute.orchestrator.usage;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class UsageCollector {
  @Scheduled(fixedDelay = 15000)
  public void poll(){ }
}
