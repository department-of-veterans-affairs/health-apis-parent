package gov.va.api.health.autoconfig.logging;

import static gov.va.api.health.autoconfig.logging.LogSanitizer.sanitize;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.Test;

public class LogSanitizerTest {
  @Test
  public void actionsChain() {
    assertThat(
            LogSanitizer.builder()
                .actions(List.of(s -> "3" + s, s -> "2" + s, s -> "1" + s))
                .build()
                .apply("xyz"))
        .isEqualTo("123xyz");
  }

  @Test
  public void actionsThatReturnNullAbort() {
    assertThat(
            LogSanitizer.builder()
                .actions(List.of(String::toLowerCase, s -> null, String::toUpperCase))
                .build()
                .apply("xyz"))
        .isNull();
  }

  @Test
  public void sanitizes() {
    assertThat(sanitize(null)).isNull();
    assertThat(sanitize("xyz")).isEqualTo("xyz");
    assertThat(sanitize("\rx\ny\rz\n")).isEqualTo(" x y z ");
  }
}
