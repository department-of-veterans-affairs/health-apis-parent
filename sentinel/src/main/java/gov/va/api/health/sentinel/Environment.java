package gov.va.api.health.sentinel;

import java.util.Locale;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum Environment {
  LAB,
  LOCAL,
  PROD,
  QA,
  STAGING,
  STAGING_LAB;

  static {
    log.info(
        "Using {} Sentinel environment "
            + "(Override with -Dsentinel=LAB|LOCAL|PRODUCTION|QA|STAGING|STAGING-LAB)",
        sentinelProperty());
  }

  /** Parse the system property 'sentinel' into the appropriate enum. */
  public static Environment get() {
    switch (sentinelProperty()) {
      case "LAB":
        return Environment.LAB;
      case "LOCAL":
        return Environment.LOCAL;
      case "PROD": // FALL-THROUGH
      case "PRODUCTION":
        return Environment.PROD;
      case "QA":
        return Environment.QA;
      case "STAGING":
        return Environment.STAGING;
      case "STAGING_LAB": // FALL-THROUGH
      case "STAGING-LAB":
        return Environment.STAGING_LAB;
      default:
        throw new IllegalArgumentException("Unknown sentinel environment: " + sentinelProperty());
    }
  }

  private static String sentinelProperty() {
    return System.getProperty("sentinel", "LOCAL").toUpperCase(Locale.ENGLISH);
  }
}
