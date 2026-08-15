package com.yourco.compute.adapters.runpod;

import com.yourco.compute.adapters.core.*;
import com.yourco.compute.domain.model.Job;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Off unless {@code providers.runpod.enabled} says otherwise, because this repository ships no
 * RunPod service to talk to and a quote for an unreachable adapter fails the whole submit.
 */
@Component
@ConditionalOnProperty(name = "providers.runpod.enabled", havingValue = "true")
public class RunPodClient implements ProviderClient {
  private final RestTemplate rest;
  private final String base;

  public RunPodClient(RestTemplateBuilder builder, @Value("${providers.runpod.base-url}") String base) {
    this.rest = builder
        .setConnectTimeout(Duration.ofSeconds(5))
        .setReadTimeout(Duration.ofSeconds(30))
        .build();
    this.base = base;
  }

  @Override public ProvisionResult provision(Job job) {
    var resp = rest.postForEntity(base+"/provision", job.getId(), String.class);
    return new ProvisionResult(resp.getBody());
  }
  @Override public void start(String instanceId) { rest.postForEntity(base+"/start", instanceId, Void.class); }
  @Override public void stop(String instanceId)  { rest.postForEntity(base+"/stop", instanceId, Void.class); }
  @Override public UsageReport collectUsage(String instanceId) {
    var r = rest.getForEntity(base+"/usage/"+instanceId, UsageReport.class);
    return r.getBody();
  }
}
