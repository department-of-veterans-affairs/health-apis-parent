package gov.va.api.health.autoconfig.logging;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;

public class LogSanitizer implements Function<String, String> {

  /** A default, shared instance. */
  private static final LogSanitizer INSTANCE =
      LogSanitizer.builder().action(preventCrlfInjection()).build();

  /**
   * Collection of actions to take when sanitizing strings. Actions will be invoked in order,
   * feeding the output of one action into next action. Should an action return null, the action
   * chain is immediately terminated.
   */
  private final List<Function<String, String>> actions;

  @SuppressWarnings("unchecked")
  @Builder
  private LogSanitizer(@NonNull @Singular List<Function<String, String>> actions) {
    this.actions = new ArrayList<>(actions);
  }

  /** Get a default instance. */
  public static LogSanitizer get() {
    return INSTANCE;
  }

  /** An action that removes newlines. */
  public static Function<String, String> preventCrlfInjection() {
    return s -> s.replace('\n', ' ').replace('\r', ' ');
  }

  /** Sanitize the given string with default rules. */
  public static String sanitize(String in) {
    return get().apply(in);
  }

  @Override
  public String apply(String in) {
    String out = in;
    for (Function<String, String> action : actions) {
      if (out == null) {
        return null;
      }
      out = action.apply(out);
    }
    return out;
  }
}
