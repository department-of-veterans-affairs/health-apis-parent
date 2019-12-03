package gov.va.api.health.sentinel;

import static com.google.common.base.Preconditions.checkState;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import lombok.experimental.UtilityClass;
import org.slf4j.LoggerFactory;

@UtilityClass
public final class SentinelProperties {

  private static final ReducedSpamLogger log =
      ReducedSpamLogger.builder().logger(LoggerFactory.getLogger(SentinelProperties.class)).build();

  /** Supplies system property access-token, or throws exception if it doesn't exist. */
  public static String magicAccessToken() {
    final String magic = System.getProperty("access-token");
    checkState(!isBlank(magic), "Access token not specified, -Daccess-token=<value>");
    return magic;
  }

  /** Read api-path from a system property. */
  public static String optionApiPath(String name, String defaultValue) {
    String property = "sentinel." + name + ".api-path";
    String apiPath = System.getProperty(property, defaultValue);
    if (!apiPath.startsWith("/")) {
      apiPath = "/" + apiPath;
    }
    if (!apiPath.endsWith("/")) {
      apiPath = apiPath + "/";
    }
    log.infoOnce("Using {} api path {} (Override with -D{}=<url>)", name, apiPath, property);
    return apiPath;
  }

  /** Read url from a system property. */
  public static String optionUrl(String name, String defaultValue) {
    String property = "sentinel." + name + ".url";
    String url = System.getProperty(property, defaultValue);
    if (url.endsWith("/")) {
      url = url.substring(0, url.length() - 1);
    }
    log.infoOnce("Using {} url {} (Override with -D{}=<url>)", name, url, property);
    return url;
  }

  /** Read thread count from system property. */
  public static int threadCount(String name, int defaultThreadCount) {
    int threads = defaultThreadCount;
    String maybeNumber = System.getProperty(name);
    if (isNotBlank(maybeNumber)) {
      try {
        threads = Integer.parseInt(maybeNumber);
      } catch (NumberFormatException e) {
        log.warn("Bad thread count {}, assuming {}", maybeNumber, threads);
      }
    }
    log.infoOnce("Using {} threads (Override with -D{}=<number>)", threads, name);
    return threads;
  }
}
