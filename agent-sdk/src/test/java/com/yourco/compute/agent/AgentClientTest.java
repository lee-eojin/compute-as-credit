package com.yourco.compute.agent;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AgentClientTest {
  private final RestTemplate rest = new RestTemplate();
  private final MockRestServiceServer server = MockRestServiceServer.bindTo(rest).build();
  private final AgentClient client = new AgentClient("http://api.test", () -> "token-abc", rest);

  @Test
  void submitCarriesBearerTokenAndIdempotencyKey() {
    server.expect(requestTo("http://api.test/v1/jobs"))
        .andExpect(header("Authorization", "Bearer token-abc"))
        .andExpect(header("Idempotency-Key", "retry-me"))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andRespond(withSuccess("{\"jobId\":7,\"status\":\"RUNNING\"}", MediaType.APPLICATION_JSON));

    assertThat(client.submitJob(new AgentClient.JobRequest("{}", "{}", 5.0), "retry-me"))
        .isEqualTo(new AgentClient.JobResponse(7L, "RUNNING"));
    server.verify();
  }

  @Test
  void anAbsentBudgetIsSentAsNullSoTheServerAppliesNoCap() {
    server.expect(requestTo("http://api.test/v1/jobs"))
        .andExpect(content().string(containsString("\"maxBudget\":null")))
        .andRespond(withSuccess("{\"jobId\":7,\"status\":\"RUNNING\"}", MediaType.APPLICATION_JSON));

    client.submitJob(new AgentClient.JobRequest("{}", "{}", null), "k");
    server.verify();
  }

  @Test
  void getCarriesBearerTokenAndReadsProviderId() {
    server.expect(requestTo("http://api.test/v1/jobs/7"))
        .andExpect(header("Authorization", "Bearer token-abc"))
        .andRespond(withSuccess("{\"jobId\":7,\"status\":\"RUNNING\",\"providerId\":3}", MediaType.APPLICATION_JSON));

    assertThat(client.getJob(7)).isEqualTo(new AgentClient.JobStatus(7L, "RUNNING", 3L));
    server.verify();
  }

  @Test
  void anEmptyResponseBodyFailsWithTheCallInTheMessage() {
    server.expect(requestTo("http://api.test/v1/jobs"))
        .andRespond(withStatus(HttpStatus.NO_CONTENT));

    assertThatThrownBy(() -> client.submitJob(new AgentClient.JobRequest("{}", "{}", 1.0), "k"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("POST /v1/jobs")
        .hasMessageContaining("empty body");
  }

  @Test
  void aMissingTokenFailsBeforeTheRequestIsSent() {
    AgentClient tokenless = new AgentClient("http://api.test", () -> null, rest);

    assertThatThrownBy(() -> tokenless.getJob(1))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("no token");
    server.verify();
  }

  @Test
  void theDefaultTemplateCanSpeakJsonAndGivesUpOnASilentServer() {
    RestTemplate template = AgentClient.defaultRestTemplate();

    assertThat(template.getMessageConverters())
        .anyMatch(converter -> converter.getClass().getSimpleName().startsWith("MappingJackson2"));
    assertThat(template.getRequestFactory()).isInstanceOf(SimpleClientHttpRequestFactory.class);
    assertThat(ReflectionTestUtils.getField(template.getRequestFactory(), "connectTimeout")).isEqualTo(5_000);
    assertThat(ReflectionTestUtils.getField(template.getRequestFactory(), "readTimeout")).isEqualTo(30_000);
  }
}
