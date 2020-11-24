package gov.va.api.health.sentinel;

import static com.google.common.base.Preconditions.checkState;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import gov.va.api.health.sentinel.SentinelProperties.ServiceDefinitionConfiguration.ServiceDefinitionConfigurationBuilder;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Delegate;
import lombok.experimental.UtilityClass;
import org.slf4j.LoggerFactory;

@UtilityClass
public final class SentinelProperties {
  private static final ReducedSpamLogger log =
      ReducedSpamLogger.builder().logger(LoggerFactory.getLogger(SentinelProperties.class)).build();

  /**
   * Create a builder that will automatically configure a ServiceDefinition based on properties, if
   * available.
   */
  public static ServiceDefinitionConfigurationBuilder forName(@NonNull String name) {
    return ServiceDefinitionConfiguration.builder().name(name);
  }

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

  /** Generically read an integer system property using a full property name. */
  public static int optionInt(String fullPropertyName, int defaultValue) {
    String maybeNumber = System.getProperty(fullPropertyName);
    int value = defaultValue;
    if (isNotBlank(maybeNumber)) {
      try {
        value = Integer.parseInt(maybeNumber);
      } catch (NumberFormatException e) {
        log.warn("Bad value for {} = {}, assuming {}", fullPropertyName, maybeNumber, defaultValue);
      }
    }
    log.infoOnce(
        "Using {} for {} (Override with -D{}=<number>)", value, fullPropertyName, fullPropertyName);
    return value;
  }

  /** Read port from a system property. */
  public static int optionPort(String name, int defaultValue) {
    return optionInt("sentinel." + name + ".port", defaultValue);
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
    return optionInt(name, defaultThreadCount);
  }

  /** Support for building a service definition with overrides from system properties. */
  @Value
  public static class ServiceDefinitionConfiguration {
    @Delegate ServiceDefinition serviceDefinition;

    @Builder
    ServiceDefinitionConfiguration(
        @NonNull String name,
        @NonNull String defaultUrl,
        int defaultPort,
        @NonNull String defaultApiPath,
        @NonNull Supplier<Optional<String>> accessToken) {
      serviceDefinition =
          ServiceDefinition.builder()
              .url(optionUrl(name, defaultUrl))
              .port(optionPort(name, defaultPort))
              .apiPath(optionApiPath(name, defaultApiPath))
              .accessToken(accessToken)
              .build();
    }
  }
}
