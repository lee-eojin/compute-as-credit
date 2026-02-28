package com.yourco.compute.adapters.core;

import com.yourco.compute.domain.model.Job;

public interface ProviderClient {
  ProvisionResult provision(Job job);
  void start(String instanceId);
  void stop(String instanceId);
  UsageReport collectUsage(String instanceId);
}
