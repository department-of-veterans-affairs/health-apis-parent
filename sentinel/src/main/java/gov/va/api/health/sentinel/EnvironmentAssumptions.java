package gov.va.api.health.sentinel;

import static org.assertj.core.api.Assumptions.assumeThat;

public class EnvironmentAssumptions {

  /** Assume environment is the provided environment. */
  public static void assumeEnvironmentIn(Environment... envs) {
    assumeThat(envs)
        .overridingErrorMessage("Skipping in " + Environment.get())
        .contains(Environment.get());
  }

  /** Assume environment is not the provided environment(s). */
  public static void assumeEnvironmentNotIn(Environment... envs) {
    assumeThat(envs)
        .overridingErrorMessage("Skipping in " + Environment.get())
        .doesNotContain(Environment.get());
  }
}
