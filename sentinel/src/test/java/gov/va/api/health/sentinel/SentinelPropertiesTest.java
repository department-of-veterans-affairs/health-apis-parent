package gov.va.api.health.sentinel;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class SentinelPropertiesTest {

  @Test
  void serviceDefinitionUsesDefaultsWhenPropertyIsNotAvailable() {
    String name = "x" + System.nanoTime();
    Supplier<Optional<String>> accessToken = () -> Optional.empty();
    var actual =
        SentinelProperties.forName(name)
            .accessToken(accessToken)
            .defaultPort(1234)
            .defaultApiPath("/ok/man")
            .defaultUrl("http://ok.com")
            .build()
            .serviceDefinition();

    assertThat(actual)
        .isEqualTo(
            ServiceDefinition.builder()
                .accessToken(accessToken)
                .port(1234)
                .apiPath("/ok/man/")
                .url("http://ok.com")
                .build());
  }

  @Test
  void serviceDefinitionUsesOverridesWhenPropertyIsAvailable() {
    String name = "y" + System.nanoTime();
    System.setProperty("sentinel." + name + ".url", "http://the-dude.com");
    System.setProperty("sentinel." + name + ".port", "5678");
    System.setProperty("sentinel." + name + ".api-path", "/im/the/dude/man");
    Supplier<Optional<String>> accessToken = () -> Optional.empty();
    var actual =
        SentinelProperties.forName(name)
            .accessToken(accessToken)
            .defaultPort(1234)
            .defaultApiPath("/ok/man")
            .defaultUrl("http://ok.com")
            .build()
            .serviceDefinition();

    assertThat(actual)
        .isEqualTo(
            ServiceDefinition.builder()
                .accessToken(accessToken)
                .port(5678)
                .apiPath("/im/the/dude/man/")
                .url("http://the-dude.com")
                .build());
  }
}
