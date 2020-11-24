package gov.va.api.health.sentinel;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OauthRobotProperties {
  private static Properties properties;

  OauthRobotProperties(String pathname) {
    loadProperties(pathname);
  }

  @SneakyThrows
  private static void loadProperties(String pathname) {
    if (pathname == null) {
      properties = System.getProperties();
      return;
    }
    File file = new File(pathname);
    if (file.exists()) {
      log.info("Loading properties from: {}", file);
      properties = new Properties(System.getProperties());
      try (FileInputStream inputStream = new FileInputStream(file)) {
        properties.load(inputStream);
      }
    } else {
      log.info("Properties not found: {}, using System properties", file);
      properties = System.getProperties();
    }
  }

  private static List<String> optionList(String name, List<String> defaultList) {
    String value = properties.getProperty(name);
    if (value == null) {
      assertThat(defaultList)
          .withFailMessage("System property %s must be specified.", name)
          .isNotNull();
      return defaultList;
    }
    return Arrays.stream(value.split(",")).collect(Collectors.toList());
  }

  /** Get a property, setting it to default if it doesn't exist. */
  public static String optionString(String name, String defaultValue) {
    String value = properties.getProperty(name, defaultValue == null ? "" : defaultValue);
    assertThat(value).withFailMessage("System property %s must be specified.", name).isNotBlank();
    return value;
  }

  public static String optionString(String name) {
    return optionString(name, "");
  }

  public static OauthRobotProperties usingPropertiesFile(String pathname) {
    return new OauthRobotProperties(pathname);
  }

  public static OauthRobotProperties usingSystemProperties() {
    return new OauthRobotProperties(null);
  }

  // ToDo forPatientScopes and update LabBot.java
  /** Create a system scopes oauth robot configuration builder. */
  public SystemOauthConfiguration.SystemOauthConfigurationBuilder forSystemOauth() {
    return SystemOauthConfiguration.builder();
  }

  @Value
  public static class SystemOauthConfiguration {
    @Delegate SystemOauthRobot.Configuration systemOauthConfig;

    @Builder
    SystemOauthConfiguration(
        String defaultTokenUrl, String defaultAudience, List<String> defaultScopes) {
      systemOauthConfig =
          SystemOauthRobot.Configuration.builder()
              .audience(optionString("system-oauth-robot.aud", defaultAudience))
              .tokenUrl(optionString("system-oauth-robot.token-url", defaultTokenUrl))
              .scopes(
                  ImmutableSet.copyOf(optionList("system-oauth-robot.scopes-csv", defaultScopes)))
              .clientId(optionString("system-oauth-robot.client-id"))
              .clientSecret(optionString("system-oauth-robot.client-secret"))
              .build();
    }
  }
}
