package com.yourco.compute.agent;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

public class AgentClient {
  private final RestTemplate rest = new RestTemplate();
  private final String baseUrl;
  public AgentClient(String baseUrl){ this.baseUrl = baseUrl; }
}
