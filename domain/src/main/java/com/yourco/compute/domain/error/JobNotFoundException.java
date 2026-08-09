package com.yourco.compute.domain.error;

public class JobNotFoundException extends RuntimeException {
  public JobNotFoundException(long jobId) {
    super("Job not found: " + jobId);
  }
}
