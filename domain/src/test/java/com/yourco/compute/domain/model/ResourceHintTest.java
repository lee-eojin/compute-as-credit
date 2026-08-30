package com.yourco.compute.domain.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourco.compute.domain.error.UnsupportedResourceHintException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResourceHintTest {
  private final ObjectMapper mapper = new ObjectMapper();

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   ", "{}"})
  void anAbsentOrEmptyHintFallsBackToThePlatformDefaults(String json) {
    ResourceHint hint = ResourceHint.parse(mapper, json);

    assertThat(hint.region()).isEqualTo(ResourceHint.DEFAULT_REGION);
    assertThat(hint.gpuType()).isEqualTo(ResourceHint.DEFAULT_GPU_TYPE);
  }

  @Test
  void aSupportedRegionAndGpuTypeSurviveParsing() {
    ResourceHint hint = ResourceHint.parse(mapper, "{\"region\":\"eu-west-1\",\"gpuType\":\"H100-80G\"}");

    assertThat(hint.region()).isEqualTo("eu-west-1");
    assertThat(hint.gpuType()).isEqualTo("H100-80G");
  }

  @Test
  void fieldsTheHintDoesNotDefineAreIgnoredRatherThanRejected() {
    ResourceHint hint = ResourceHint.parse(mapper, "{\"region\":\"us-west-2\",\"spotOk\":true}");

    assertThat(hint.region()).isEqualTo("us-west-2");
    assertThat(hint.gpuType()).isEqualTo(ResourceHint.DEFAULT_GPU_TYPE);
  }

  @Test
  void aRegionThePlatformDoesNotBrokerIsRejected() {
    assertThatThrownBy(() -> ResourceHint.parse(mapper, "{\"region\":\"mars-north-1\"}"))
        .isInstanceOf(UnsupportedResourceHintException.class)
        .hasMessageContaining("mars-north-1");
  }

  @Test
  void aGpuTypeThePlatformDoesNotBrokerIsRejected() {
    assertThatThrownBy(() -> ResourceHint.parse(mapper, "{\"gpuType\":\"RTX-4090\"}"))
        .isInstanceOf(UnsupportedResourceHintException.class)
        .hasMessageContaining("RTX-4090");
  }

  @Test
  void aStreamOfDistinctRegionsCannotMintDistinctQuoteKeys() {
    for (int i = 0; i < 100; i++) {
      String json = "{\"region\":\"filler-" + i + "\"}";
      assertThatThrownBy(() -> ResourceHint.parse(mapper, json))
          .isInstanceOf(UnsupportedResourceHintException.class);
    }

    assertThat(ResourceHint.SUPPORTED_REGIONS.size() * ResourceHint.SUPPORTED_GPU_TYPES.size())
        .isLessThan(100);
  }

  @Test
  void malformedJsonIsReportedAsSuch() {
    assertThatThrownBy(() -> ResourceHint.parse(mapper, "{not json"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not valid JSON");
  }

  @Test
  void theQuoteKeyIsBuiltFromBothValidatedFields() {
    assertThat(new ResourceHint("us-west-2", "L40S").quoteKey()).isEqualTo("us-west-2|L40S");
  }
}
