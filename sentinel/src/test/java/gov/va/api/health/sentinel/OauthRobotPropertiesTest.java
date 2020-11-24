package gov.va.api.health.sentinel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class OauthRobotPropertiesTest {
  @Test
  void oauthPropertiesFailsWhenNoSystemPropertiesSpecified() {
    assertThatExceptionOfType(AssertionError.class)
        .isThrownBy(
            () ->
                OauthRobotProperties.usingSystemProperties()
                    .forSystemOauth()
                    .defaultAudience("http://lebowski.com")
                    .defaultScopes(List.of("system/NoCool.read"))
                    .defaultTokenUrl("http://nottoday.com")
                    .build()
                    .systemOauthConfig());
  }

  @Test
  void oauthPropertiesUsesDefaultsWhenPropertyIsUnavailable() {
    System.setProperty("system-oauth-robot.client-id", "clientId");
    System.setProperty("system-oauth-robot.client-secret", "clientSecret");
    var actual =
        OauthRobotProperties.usingSystemProperties()
            .forSystemOauth()
            .defaultAudience("http://the-dude.com")
            .defaultScopes(List.of("system/Nachos.read"))
            .defaultTokenUrl("http://tokenz.com")
            .build()
            .systemOauthConfig();
    // Expiration is determined based on Instant.now(), so we can't assert equality as a whole
    assertThat(actual.audience()).isEqualTo("http://the-dude.com");
    assertThat(actual.tokenUrl()).isEqualTo("http://tokenz.com");
    assertThat(actual.scopes()).isEqualTo(Set.of("system/Nachos.read"));
    assertThat(actual.clientId()).isEqualTo("clientId");
    assertThat(actual.clientSecret()).isEqualTo("clientSecret");
    assertThat(actual.expiration()).isNotNull();
  }

  @Test
  void oauthPropertiesUsesOverridesWhenFileIsAvailable() {
    var actual =
        OauthRobotProperties.usingPropertiesFile("src/test/resources/oauth-robot-test.properties")
            .forSystemOauth()
            .defaultAudience("http://lebowski.com")
            .defaultScopes(List.of("system/NoCool.read"))
            .defaultTokenUrl("http://nottoday.com")
            .build()
            .systemOauthConfig();
    // Expiration is determined based on Instant.now(), so we can't assert equality as a whole
    assertThat(actual.audience()).isEqualTo("http://the-dude.com");
    assertThat(actual.tokenUrl()).isEqualTo("http://tokenz.com");
    assertThat(actual.scopes()).isEqualTo(Set.of("system/Nachos.read"));
    assertThat(actual.clientId()).isEqualTo("clientId");
    assertThat(actual.clientSecret()).isEqualTo("clientSecret");
    assertThat(actual.expiration()).isNotNull();
  }

  @Test
  void oauthPropertiesUsesOverridesWhenPropertyIsAvailable() {
    System.setProperty("system-oauth-robot.aud", "http://the-dude.com");
    System.setProperty("system-oauth-robot.token-url", "http://tokenz.com");
    System.setProperty("system-oauth-robot.scopes-csv", "system/Nachos.read");
    System.setProperty("system-oauth-robot.client-id", "clientId");
    System.setProperty("system-oauth-robot.client-secret", "clientSecret");
    var actual =
        OauthRobotProperties.usingSystemProperties()
            .forSystemOauth()
            .defaultAudience("http://lebowski.com")
            .defaultScopes(List.of("system/NoCool.read"))
            .defaultTokenUrl("http://nottoday.com")
            .build()
            .systemOauthConfig();
    // Expiration is determined based on Instant.now(), so we can't assert equality as a whole
    assertThat(actual.audience()).isEqualTo("http://the-dude.com");
    assertThat(actual.tokenUrl()).isEqualTo("http://tokenz.com");
    assertThat(actual.scopes()).isEqualTo(Set.of("system/Nachos.read"));
    assertThat(actual.clientId()).isEqualTo("clientId");
    assertThat(actual.clientSecret()).isEqualTo("clientSecret");
    assertThat(actual.expiration()).isNotNull();
  }

  @Test
  void oauthPropertiesUsesSystemPropertiesWhenFileIsNotAvailable() {
    System.setProperty("system-oauth-robot.aud", "http://the-dude.com");
    System.setProperty("system-oauth-robot.token-url", "http://tokenz.com");
    System.setProperty("system-oauth-robot.scopes-csv", "system/Nachos.read");
    System.setProperty("system-oauth-robot.client-id", "clientId");
    System.setProperty("system-oauth-robot.client-secret", "clientSecret");
    var actual =
        OauthRobotProperties.usingPropertiesFile("src/test/resources/nope.properties")
            .forSystemOauth()
            .defaultAudience("http://lebowski.com")
            .defaultScopes(List.of("system/NoCool.read"))
            .defaultTokenUrl("http://nottoday.com")
            .build()
            .systemOauthConfig();
    // Expiration is determined based on Instant.now(), so we can't assert equality as a whole
    assertThat(actual.audience()).isEqualTo("http://the-dude.com");
    assertThat(actual.tokenUrl()).isEqualTo("http://tokenz.com");
    assertThat(actual.scopes()).isEqualTo(Set.of("system/Nachos.read"));
    assertThat(actual.clientId()).isEqualTo("clientId");
    assertThat(actual.clientSecret()).isEqualTo("clientSecret");
    assertThat(actual.expiration()).isNotNull();
  }
}
