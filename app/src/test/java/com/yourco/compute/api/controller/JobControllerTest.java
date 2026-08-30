package com.yourco.compute.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourco.compute.api.error.ApiExceptionHandler;
import com.yourco.compute.api.security.SecurityConfig;
import com.yourco.compute.api.service.JobSubmissionService;
import com.yourco.compute.domain.error.BudgetExceededException;
import com.yourco.compute.domain.error.JobNotFoundException;
import com.yourco.compute.domain.error.UnsupportedResourceHintException;
import com.yourco.compute.domain.model.Job;
import com.yourco.compute.domain.model.JobStatus;
import com.yourco.compute.domain.model.ResourceHint;
import com.yourco.compute.orchestrator.service.JobOrchestrator;
import com.yourco.compute.orchestrator.storage.StorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(JobController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class})
@TestPropertySource(properties = "security.jwt.secret=test-secret-that-is-long-enough-for-hs256")
class JobControllerTest {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Autowired MockMvc mvc;
  @MockBean JobSubmissionService submissions;
  @MockBean JobOrchestrator orchestrator;
  @MockBean StorageService storage;

  @Test
  void theJobIsBilledToTheTokenSubject() throws Exception {
    acceptSubmissions();

    mvc.perform(asUser("42", post("/v1/jobs").contentType(APPLICATION_JSON).content(body())))
        .andExpect(status().isOk());

    assertThat(submitted().getUserId()).isEqualTo(42L);
  }

  @Test
  void aUserIdInTheBodyIsIgnoredRatherThanBilled() throws Exception {
    acceptSubmissions();
    String spoofed = """
        {"userId":999,"agentSpec":"{}","resourceHint":"{}","maxBudget":50.0}""";

    mvc.perform(asUser("42", post("/v1/jobs").contentType(APPLICATION_JSON).content(spoofed)))
        .andExpect(status().isOk());

    assertThat(submitted().getUserId()).isEqualTo(42L);
  }

  @ParameterizedTest
  @ValueSource(strings = {"user1", "+42", " 42", "042", "٤٢", "99999999999999999999", ""})
  void aSubjectThatIsNotOneCanonicalUserIdCannotSubmit(String subject) throws Exception {
    mvc.perform(asUser(subject, post("/v1/jobs").contentType(APPLICATION_JSON).content(body())))
        .andExpect(status().isForbidden());

    verify(submissions, never()).submit(any(), anyString());
  }

  @Test
  void agentSpecThatIsNotAJsonObjectIsRejectedBeforeItReachesTheDatabase() throws Exception {
    postExpectingBadRequest(body("pytorch-trainer", "{}", 50.0));
  }

  @Test
  void agentSpecWithTrailingJunkIsRejectedBeforeItReachesTheDatabase() throws Exception {
    postExpectingBadRequest(body("{\"image\":\"x\"} junk", "{}", 50.0));
  }

  @Test
  void anEmptyBodyIsRejectedBecauseAgentSpecIsRequired() throws Exception {
    postExpectingBadRequest("{}");
  }

  @Test
  void aBudgetTooLargeForTheColumnIsRejectedInsteadOfCrashing() throws Exception {
    postExpectingBadRequest("""
        {"agentSpec":"{}","resourceHint":"{}","maxBudget":1e309}""");
  }

  @Test
  void anAgentSpecPastTheColumnBudgetIsRejectedRatherThanStored() throws Exception {
    postExpectingBadRequest(body("{\"pad\":\"" + "x".repeat(9000) + "\"}", "{}", 50.0));
  }

  @Test
  void aResourceHintPastItsLimitIsRejectedRatherThanStored() throws Exception {
    postExpectingBadRequest(body("{}", "{\"pad\":\"" + "x".repeat(600) + "\"}", 50.0));
  }

  @Test
  void theIdempotencyKeyHeaderIsHandedToTheSubmissionService() throws Exception {
    acceptSubmissions();

    mvc.perform(asUser("42", post("/v1/jobs")
            .header("Idempotency-Key", "shared-key")
            .contentType(APPLICATION_JSON).content(body())))
        .andExpect(status().isOk());

    verify(submissions).submit(any(), eq("shared-key"));
  }

  @Test
  void aRequestWithoutTheHeaderSubmitsWithNoKeyAtAll() throws Exception {
    acceptSubmissions();

    mvc.perform(asUser("42", post("/v1/jobs").contentType(APPLICATION_JSON).content(body())))
        .andExpect(status().isOk());

    verify(submissions).submit(any(), isNull());
  }

  @Test
  void anIdempotencyKeyTooLongForTheColumnIsRejectedBeforeAnythingIsCharged() throws Exception {
    mvc.perform(asUser("42", post("/v1/jobs")
            .header("Idempotency-Key", "k".repeat(65))
            .contentType(APPLICATION_JSON).content(body())))
        .andExpect(status().isBadRequest());

    verify(submissions, never()).submit(any(), anyString());
  }

  @Test
  void aBlankIdempotencyKeyIsRejectedRatherThanStored() throws Exception {
    mvc.perform(asUser("42", post("/v1/jobs")
            .header("Idempotency-Key", "")
            .contentType(APPLICATION_JSON).content(body())))
        .andExpect(status().isBadRequest());

    verify(submissions, never()).submit(any(), anyString());
  }

  @Test
  void aHoldOverTheBudgetIsReportedAsUnprocessable() throws Exception {
    given(submissions.submit(any(), any()))
        .willThrow(new BudgetExceededException(new BigDecimal("0.744"), new BigDecimal("0.50")));

    mvc.perform(asUser("42", post("/v1/jobs").contentType(APPLICATION_JSON).content(body())))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void aRegionThePlatformDoesNotBrokerIsReportedAsABadRequest() throws Exception {
    given(submissions.submit(any(), any())).willThrow(
        new UnsupportedResourceHintException("region", "mars-north-1", ResourceHint.SUPPORTED_REGIONS));

    mvc.perform(asUser("42", post("/v1/jobs").contentType(APPLICATION_JSON).content(body())))
        .andExpect(status().isBadRequest());
  }

  @Test
  void aKeyThatLostTheRaceIsReportedAsAConflictSoTheCallerRetries() throws Exception {
    given(submissions.submit(any(), any()))
        .willThrow(new DataIntegrityViolationException("uk_idempotency_scope_user"));

    mvc.perform(asUser("42", post("/v1/jobs")
            .header("Idempotency-Key", "shared-key")
            .contentType(APPLICATION_JSON).content(body())))
        .andExpect(status().isConflict());
  }

  @Test
  void anUnknownJobIsANotFoundRatherThanAServerError() throws Exception {
    given(orchestrator.getForUser(anyLong(), anyLong())).willThrow(new JobNotFoundException(999));

    mvc.perform(asUser("42", get("/v1/jobs/999"))).andExpect(status().isNotFound());
  }

  @Test
  void getReadsTheJobThroughTheOwnershipCheckAndReturnsTheProvider() throws Exception {
    Job job = withStatus(new Job(), JobStatus.RUNNING);
    job.setUserId(42L);
    job.setProviderId(3L);
    given(orchestrator.getForUser(7L, 42L)).willReturn(job);

    mvc.perform(asUser("42", get("/v1/jobs/7")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.providerId").value(3));
  }

  @Test
  void ioUrlsAreNotHandedOutForSomeoneElsesJob() throws Exception {
    given(orchestrator.getForUser(anyLong(), anyLong())).willThrow(new JobNotFoundException(7));

    mvc.perform(asUser("42", post("/v1/jobs/7/io"))).andExpect(status().isNotFound());

    verify(storage, never()).allocateForJob(anyLong());
  }

  @Test
  void aWriteOnlyTokenCannotProbeJobsWithHead() throws Exception {
    mvc.perform(head("/v1/jobs/7").with(jwt()
            .jwt(token -> token.subject("42"))
            .authorities(new SimpleGrantedAuthority("SCOPE_jobs:write"))))
        .andExpect(status().isForbidden());

    verify(orchestrator, never()).getForUser(anyLong(), anyLong());
  }

  private void acceptSubmissions() {
    given(submissions.submit(any(), any()))
        .willAnswer(inv -> withStatus(inv.getArgument(0), JobStatus.RUNNING));
  }

  private Job submitted() {
    ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
    verify(submissions).submit(captor.capture(), any());
    return captor.getValue();
  }

  private void postExpectingBadRequest(String payload) throws Exception {
    mvc.perform(asUser("42", post("/v1/jobs").contentType(APPLICATION_JSON).content(payload)))
        .andExpect(status().isBadRequest());

    verify(submissions, never()).submit(any(), anyString());
  }

  private static String body() throws Exception {
    return body("{\"image\":\"x\"}", "{\"gpuType\":\"A100-80G\"}", 50.0);
  }

  private static String body(String agentSpec, String resourceHint, Double maxBudget) throws Exception {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("agentSpec", agentSpec);
    payload.put("resourceHint", resourceHint);
    payload.put("maxBudget", maxBudget);
    return MAPPER.writeValueAsString(payload);
  }

  private static MockHttpServletRequestBuilder asUser(String subject, MockHttpServletRequestBuilder request) {
    return request.with(jwt()
        .jwt(token -> token.subject(subject))
        .authorities(new SimpleGrantedAuthority("SCOPE_jobs:write"), new SimpleGrantedAuthority("SCOPE_jobs:read")));
  }

  private static Job withStatus(Job job, JobStatus status) {
    job.setStatus(status);
    return job;
  }
}
