package com.yourco.compute.api.security;

import com.yourco.compute.ApiGatewayApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * application.yml deliberately carries no fallback for the secret. With one, the property was never
 * blank, the guard below could not fire, and a deployment that forgot JWT_SECRET came up looking
 * healthy while answering 401 to everything.
 */
class SecurityConfigStartupTest {

  private static final Pattern CONFIGURED_SECRET =
      Pattern.compile("secret:\\s*\\$\\{JWT_SECRET:([^}]*)}");

  @Test
  void theShippedConfigurationHoldsNoFallbackSecret() throws Exception {
    String config = new String(
        getClass().getResourceAsStream("/application.yml").readAllBytes(), StandardCharsets.UTF_8);

    Matcher configured = CONFIGURED_SECRET.matcher(config);
    assertThat(configured.find()).as("security.jwt.secret placeholder").isTrue();
    assertThat(configured.group(1))
        .as("a fallback here would make the startup guard below unreachable")
        .isBlank();
  }

  @Test
  void anUnsetSecretStopsStartupRatherThanFailingEveryRequestLater() {
    assertThatThrownBy(() -> new SpringApplicationBuilder(ApiGatewayApplication.class)
        .profiles("test")
        // Command line args, not properties(): the latter are defaults and the test profile wins.
        .run("--security.jwt.secret=", "--server.port=0")
        .close())
        .rootCause()
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("JWT_SECRET");
  }

  @Test
  void aSecretTooShortForTheAlgorithmStopsStartupToo() {
    assertThatThrownBy(() -> new SpringApplicationBuilder(ApiGatewayApplication.class)
        .profiles("test")
        .run("--security.jwt.secret=too-short", "--server.port=0")
        .close())
        .rootCause()
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("at least 32");
  }
}
