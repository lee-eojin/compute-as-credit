package com.yourco.compute.domain.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Region and GPU type a job asked for, with the platform defaults filled in for anything omitted. */
public record ResourceHint(String region, String gpuType) {
  public static final String DEFAULT_REGION = "us-east-1";
  public static final String DEFAULT_GPU_TYPE = "A100-80G";

  public static ResourceHint parse(ObjectMapper mapper, String json) {
    if (json == null || json.isBlank()) {
      return new ResourceHint(DEFAULT_REGION, DEFAULT_GPU_TYPE);
    }
    JsonNode node;
    try {
      node = mapper.readTree(json);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("resourceHint is not valid JSON", e);
    }
    return new ResourceHint(text(node, "region", DEFAULT_REGION), text(node, "gpuType", DEFAULT_GPU_TYPE));
  }

  private static String text(JsonNode node, String field, String fallback) {
    JsonNode value = node.get(field);
    if (value == null || !value.isTextual() || value.asText().isBlank()) {
      return fallback;
    }
    return value.asText();
  }
}
