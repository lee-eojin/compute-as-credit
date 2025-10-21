package com.yourco.compute.orchestrator.storage;

import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

@Service
public class StorageService {
  public record IOUrls(String uploadUrl, String downloadUrl, String inputUri, String outputUri, Instant expiresAt){}

  public IOUrls allocateForJob(long jobId){
    String base = "https://storage.internal";
    String token = URLEncoder.encode("token-"+jobId, StandardCharsets.UTF_8);
    String up = base + "/upload?job=" + jobId + "&t=" + token;
    String down = base + "/download?job=" + jobId + "&t=" + token;
    String input = "s3://tenant/"+jobId+"/input/";
    String output = "s3://tenant/"+jobId+"/output/";
    return new IOUrls(up, down, input, output, Instant.now().plusSeconds(3600));
  }
}
