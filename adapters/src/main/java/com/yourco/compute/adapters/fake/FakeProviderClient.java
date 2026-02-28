package com.yourco.compute.adapters.fake;

import com.yourco.compute.adapters.core.*;
import com.yourco.compute.domain.model.Job;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class FakeProviderClient implements ProviderClient {
  @Override public ProvisionResult provision(Job job) { return new ProvisionResult("fake-"+UUID.randomUUID()); }
  @Override public void start(String instanceId) { /* no-op */ }
  @Override public void stop(String instanceId) { /* no-op */ }
  @Override public UsageReport collectUsage(String instanceId) { return new UsageReport(600, 7.23); }
}
