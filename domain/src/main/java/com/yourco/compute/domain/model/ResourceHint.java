package com.yourco.compute.domain.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourco.compute.domain.error.UnsupportedResourceHintException;

import java.util.Set;

/**
 * Region and GPU type a job asked for, with the platform defaults filled in for anything omitted.
 *
 * <p>Both values are checked against what the platform actually brokers. That is not only input
 * hygiene: the quote cache is keyed on them, so free-form values would let one caller mint
 * unlimited distinct keys and evict every real entry.
 */
public record ResourceHint(String region, String gpuType) {
  public static final String DEFAULT_REGION = "us-east-1";
  public static final String DEFAULT_GPU_TYPE = "A100-80G";

  public static final Set<String> SUPPORTED_REGIONS =
      Set.of("us-east-1", "us-west-2", "eu-west-1", "ap-northeast-2");
  public static final Set<String> SUPPORTED_GPU_TYPES =
      Set.of("A100-80G", "H100-80G", "L40S", "A10G");

  public ResourceHint {
    if (!SUPPORTED_REGIONS.contains(region)) {
      throw new UnsupportedResourceHintException("region", region, SUPPORTED_REGIONS);
    }
    if (!SUPPORTED_GPU_TYPES.contains(gpuType)) {
      throw new UnsupportedResourceHintException("gpuType", gpuType, SUPPORTED_GPU_TYPES);
    }
  }

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

  /** The cache key for a quote lookup. Bounded by the supported sets, unlike the raw request. */
  public String quoteKey() {
    return region + "|" + gpuType;
  }

  private static String text(JsonNode node, String field, String fallback) {
    JsonNode value = node.get(field);
    if (value == null || !value.isTextual() || value.asText().isBlank()) {
      return fallback;
    }
    return value.asText();
  }
}
