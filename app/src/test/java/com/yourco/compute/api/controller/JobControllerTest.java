package com.yourco.compute.api.controller;

import com.yourco.compute.api.error.ApiExceptionHandler;
import com.yourco.compute.api.infra.IdempotencyService;
import com.yourco.compute.api.security.SecurityConfig;
import com.yourco.compute.domain.error.BudgetExceededException;
import com.yourco.compute.domain.error.JobNotFoundException;
import com.yourco.compute.domain.model.Job;
import com.yourco.compute.domain.model.JobStatus;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(JobController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class})
@TestPropertySource(properties = "security.jwt.secret=test-secret-that-is-long-enough-for-hs256")
class JobControllerTest {
  private static final String BODY = """
      {"agentSpec":"{\\"image\\":\\"x\\"}","resourceHint":"{\\"gpuType\\":\\"A100-80G\\"}","maxBudget":50.0}""";

  @Autowired MockMvc mvc;
  @MockBean JobOrchestrator orchestrator;
  @MockBean StorageService storage;
  @MockBean IdempotencyService idem;

  @Test
  void theJobIsBilledToTheTokenSubject() throws Exception {
    given(orchestrator.submit(any())).willAnswer(inv -> withStatus(inv.getArgument(0), JobStatus.RUNNING));

    mvc.perform(asUser("42", post("/v1/jobs").contentType(APPLICATION_JSON).content(BODY)))
        .andExpect(status().isOk());

    ArgumentCaptor<Job> submitted = ArgumentCaptor.forClass(Job.class);
    verify(orchestrator).submit(submitted.capture());
    assertThat(submitted.getValue().getUserId()).isEqualTo(42L);
  }

  @Test
  void aUserIdInTheBodyIsIgnoredRatherThanBilled() throws Exception {
    given(orchestrator.submit(any())).willAnswer(inv -> withStatus(inv.getArgument(0), JobStatus.RUNNING));
    String spoofed = """
        {"userId":999,"agentSpec":"{}","resourceHint":"{}","maxBudget":50.0}""";

    mvc.perform(asUser("42", post("/v1/jobs").contentType(APPLICATION_JSON).content(spoofed)))
        .andExpect(status().isOk());

    ArgumentCaptor<Job> submitted = ArgumentCaptor.forClass(Job.class);
    verify(orchestrator).submit(submitted.capture());
    assertThat(submitted.getValue().getUserId()).isEqualTo(42L);
  }

  @ParameterizedTest
  @ValueSource(strings = {"user1", "+42", " 42", "042", "٤٢", "99999999999999999999", ""})
  void aSubjectThatIsNotOneCanonicalUserIdCannotSubmit(String subject) throws Exception {
    mvc.perform(asUser(subject, post("/v1/jobs").contentType(APPLICATION_JSON).content(BODY)))
        .andExpect(status().isForbidden());

    verify(orchestrator, never()).submit(any());
  }

  @Test
  void agentSpecThatIsNotAJsonObjectIsRejectedBeforeItReachesTheDatabase() throws Exception {
    String plainText = """
        {"agentSpec":"pytorch-trainer","resourceHint":"{}","maxBudget":50.0}""";

    mvc.perform(asUser("42", post("/v1/jobs").contentType(APPLICATION_JSON).content(plainText)))
        .andExpect(status().isBadRequest());

    verify(orchestrator, never()).submit(any());
  }

  @Test
  void agentSpecWithTrailingJunkIsRejectedBeforeItReachesTheDatabase() throws Exception {
    String trailing = """
        {"agentSpec":"{\\"image\\":\\"x\\"} junk","resourceHint":"{}","maxBudget":50.0}""";

    mvc.perform(asUser("42", post("/v1/jobs").contentType(APPLICATION_JSON).content(trailing)))
        .andExpect(status().isBadRequest());

    verify(orchestrator, never()).submit(any());
  }

  @Test
  void anEmptyBodyIsRejectedBecauseAgentSpecIsRequired() throws Exception {
    mvc.perform(asUser("42", post("/v1/jobs").contentType(APPLICATION_JSON).content("{}")))
        .andExpect(status().isBadRequest());

    verify(orchestrator, never()).submit(any());
  }

  @Test
  void aBudgetTooLargeForTheColumnIsRejectedInsteadOfCrashing() throws Exception {
    String huge = """
        {"agentSpec":"{}","resourceHint":"{}","maxBudget":1e309}""";

    mvc.perform(asUser("42", post("/v1/jobs").contentType(APPLICATION_JSON).content(huge)))
        .andExpect(status().isBadRequest());

    verify(orchestrator, never()).submit(any());
  }

  @Test
  void anIdempotencyKeyIsLookedUpUnderTheCallerSoOtherTenantsKeysDoNotCollide() throws Exception {
    given(idem.findJob("shared-key", "JOB_SUBMIT", 42L)).willReturn(Optional.empty());
    given(orchestrator.submit(any())).willAnswer(inv -> withStatus(inv.getArgument(0), JobStatus.RUNNING));

    mvc.perform(asUser("42", post("/v1/jobs")
            .header("Idempotency-Key", "shared-key")
            .contentType(APPLICATION_JSON).content(BODY)))
        .andExpect(status().isOk());

    verify(idem).findJob("shared-key", "JOB_SUBMIT", 42L);
    verify(idem).remember(eq("shared-key"), eq("JOB_SUBMIT"), eq(42L), any());
    verify(orchestrator).submit(any());
  }

  @Test
  void aReplayedKeyReturnsTheOriginalJobWithoutSubmittingAgain() throws Exception {
    Job original = withStatus(new Job(), JobStatus.RUNNING);
    original.setUserId(42L);
    given(idem.findJob("shared-key", "JOB_SUBMIT", 42L)).willReturn(Optional.of(7L));
    given(orchestrator.getForUser(7L, 42L)).willReturn(original);

    mvc.perform(asUser("42", post("/v1/jobs")
            .header("Idempotency-Key", "shared-key")
            .contentType(APPLICATION_JSON).content(BODY)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("RUNNING"));

    verify(orchestrator, never()).submit(any());
  }

  @Test
  void anIdempotencyKeyTooLongForTheColumnIsRejectedBeforeAnythingIsCharged() throws Exception {
    mvc.perform(asUser("42", post("/v1/jobs")
            .header("Idempotency-Key", "k".repeat(65))
            .contentType(APPLICATION_JSON).content(BODY)))
        .andExpect(status().isBadRequest());

    verify(orchestrator, never()).submit(any());
  }

  @Test
  void aBlankIdempotencyKeyIsRejectedRatherThanStored() throws Exception {
    mvc.perform(asUser("42", post("/v1/jobs")
            .header("Idempotency-Key", "")
            .contentType(APPLICATION_JSON).content(BODY)))
        .andExpect(status().isBadRequest());

    verify(orchestrator, never()).submit(any());
  }

  @Test
  void aHoldOverTheBudgetIsReportedAsUnprocessable() throws Exception {
    given(orchestrator.submit(any()))
        .willThrow(new BudgetExceededException(new BigDecimal("0.744"), new BigDecimal("0.50")));

    mvc.perform(asUser("42", post("/v1/jobs").contentType(APPLICATION_JSON).content(BODY)))
        .andExpect(status().isUnprocessableEntity());
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
