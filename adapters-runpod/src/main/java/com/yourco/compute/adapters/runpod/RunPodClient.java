package com.yourco.compute.adapters.runpod;

import com.yourco.compute.adapters.core.*;
import com.yourco.compute.domain.model.Job;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class RunPodClient implements ProviderClient {
  private final RestTemplate rest = new RestTemplate();
  private final String base = "http://localhost:18080";

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
