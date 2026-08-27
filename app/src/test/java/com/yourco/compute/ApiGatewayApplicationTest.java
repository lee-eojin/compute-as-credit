package com.yourco.compute;

import com.yourco.compute.adapters.core.ProviderClient;
import com.yourco.compute.domain.repo.JobRepository;
import com.yourco.compute.domain.repo.OutboxEventRepository;
import com.yourco.compute.domain.repo.ProviderRepository;
import com.yourco.compute.orchestrator.service.JobOrchestrator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Entities and repositories live in other modules under this package. If the application class
 * moves back down into a subpackage, the auto-configuration package stops reaching them and the
 * context no longer starts.
 */
@SpringBootTest
@ActiveProfiles("test")
class ApiGatewayApplicationTest {

  @Autowired JobOrchestrator orchestrator;
  @Autowired JobRepository jobs;
  @Autowired ProviderRepository providers;
  @Autowired OutboxEventRepository outbox;
  @Autowired List<ProviderClient> adapters;

  @Test
  void repositoriesFromEveryModuleAreWired() {
    assertThat(orchestrator).isNotNull();
    assertThat(jobs.count()).isZero();
    assertThat(providers.findByName("FakeProviderClient")).isEmpty();
    assertThat(outbox.count()).isZero();
  }

  @Test
  void theRunPodAdapterStaysOutOfThePoolUntilItIsPointedAtSomething() {
    assertThat(adapters).extracting(a -> a.getClass().getSimpleName())
        .containsExactly("FakeProviderClient");
  }
}
