package gov.va.api.health.sentinel;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.CharStreams;
import gov.va.api.health.sentinel.LabBot.LabBotUserResult.LabBotUserResultBuilder;
import gov.va.api.health.sentinel.selenium.IdMeOauthLoginDriver;
import gov.va.api.health.sentinel.selenium.MyHealtheVetOauthLoginDriver;
import gov.va.api.health.sentinel.selenium.OauthLoginDriver;
import gov.va.api.health.sentinel.selenium.VaOauthRobot;
import gov.va.api.health.sentinel.selenium.VaOauthRobot.Configuration;
import gov.va.api.health.sentinel.selenium.VaOauthRobot.Configuration.Authorization;
import gov.va.api.health.sentinel.selenium.VaOauthRobot.Configuration.UserCredentials;
import gov.va.api.health.sentinel.selenium.VaOauthRobot.OAuthCredentialsMode;
import gov.va.api.health.sentinel.selenium.VaOauthRobot.OAuthCredentialsType;
import gov.va.api.health.sentinel.selenium.VaOauthRobot.TokenExchange;
import io.restassured.RestAssured;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.math.NumberUtils;

@Slf4j
@Value
public class LabBot {
  @NonNull List<String> scopes;

  @Getter(AccessLevel.PRIVATE)
  Config config;

  @NonNull List<String> userIds;

  /**
   * Builds what is required by LabBot.
   *
   * @param scopes scopes that LabBot will for token requests
   * @param userIds userIds that LabBot will get tokens and make requests against
   * @param configFile configFile that LabBot grabs its configuration properties from
   */
  @Builder
  public LabBot(
      @NonNull List<String> scopes, @NonNull List<String> userIds, @NonNull String configFile) {
    this.scopes = scopes;
    this.userIds = userIds;
    config = new LabBot.Config(configFile);
  }

  /** Gets all Lab users. */
  public static List<String> allUsers() {
    List<String> allUsers = new ArrayList<>(100);
    for (int i = 1; i <= 5; i++) {
      allUsers.add("vasdvp+IDME_" + String.format("%02d", i) + "@gmail.com");
    }
    for (int i = 101; i <= 183; i++) {
      allUsers.add("va.api.user+idme." + String.format("%03d", i) + "@gmail.com");
    }
    return allUsers;
  }

  private Authorization makeAuthorization(SmartOnFhirUrls urls) {
    return Authorization.builder()
        .clientId(config.clientId())
        .clientSecret(config.clientSecret())
        .authorizeUrl(urls.authorize())
        .redirectUrl(config.redirectUrl())
        .state(config.state())
        .aud(config.aud())
        .scopes(ImmutableSet.copyOf(scopes))
        .build();
  }

  /**
   * Creates VaOauthRobot with specified user credentials and urls for Lab environment using Chrome
   * Driver.
   *
   * @param userCredentials Credentials for the user to perform operations against.
   * @param baseUrl URLs to perform operations against.
   */
  private VaOauthRobot makeLabBot(
      UserCredentials userCredentials, String baseUrl, OAuthCredentialsMode credentialsMode) {
    SmartOnFhirUrls urls = new SmartOnFhirUrls(baseUrl);
    Authorization authorization = makeAuthorization(urls);
    OauthLoginDriver loginDriver =
        config.credentialsType() == OAuthCredentialsType.MY_HEALTHE_VET
            ? new MyHealtheVetOauthLoginDriver()
            : new IdMeOauthLoginDriver();
    Configuration configuration =
        Configuration.builder()
            .skipTwoFactorAuth(config.skipTwoFactorAuth())
            .authorization(authorization)
            .tokenUrl(urls.token())
            .user(userCredentials)
            .chromeDriver(config.driver())
            .oauthLoginDriver(loginDriver)
            .headless(config.headless())
            .credentialsMode(credentialsMode)
            .navigationTimeout(config.navigationTimeout())
            .build();
    return VaOauthRobot.of(configuration);
  }

  /**
   * Given a path send a request for each user. Replaces {icn} with patient icn to send request.
   * Useful for when you want to send a given request to a set of users.
   *
   * @param path The path to use for the requests, must contain {icn} to be replaced with the
   *     patient icn.
   */
  @SneakyThrows
  public List<LabBotUserResult> request(String path) {
    List<LabBotUserResult> tokenUserResultList = tokens();
    List<LabBotUserResult> responseUserResultList = new CopyOnWriteArrayList<>();
    ExecutorService ex = Executors.newFixedThreadPool(10);
    List<Future<?>> futures = new ArrayList<>(userIds.size());
    for (LabBotUserResult tokenResult : tokenUserResultList) {
      futures.add(
          ex.submit(
              () -> {
                LabBotUserResultBuilder resultBuilder =
                    LabBotUserResult.builder()
                        .user(tokenResult.user())
                        .tokenExchange(tokenResult.tokenExchange())
                        .vaOauthRobot(tokenResult.vaOauthRobot());
                try {
                  resultBuilder.response(
                      request(
                          path.replace("{icn}", tokenResult.tokenExchange.patient()),
                          tokenResult.tokenExchange.accessToken()));
                } catch (Exception e) {
                  log.error(
                      "Request failure {} {}: {}",
                      tokenResult.user().id(),
                      path,
                      e.getMessage(),
                      e.getCause());
                  resultBuilder.response(
                      "ERROR: " + e.getClass().getName() + ": " + e.getMessage());
                } finally {
                  responseUserResultList.add(resultBuilder.build());
                }
              }));
    }
    results(ex, futures);
    return responseUserResultList;
  }

  /** Send a request to the path with the access token. */
  @SneakyThrows
  public String request(String path, String accessToken) {
    URL baseUrl = new URL(config.baseUrl());
    URL url = new URL(baseUrl.getProtocol(), baseUrl.getHost(), path);
    HttpURLConnection con = (HttpURLConnection) url.openConnection();
    con.setRequestProperty("Authorization", "Bearer " + accessToken);
    con.setRequestMethod("GET");
    log.info("Sending request to: " + url.toString());
    try (var reader = new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8)) {
      return CharStreams.toString(reader);
    }
  }

  private void results(ExecutorService ex, List<Future<?>> futures) throws InterruptedException {
    ex.shutdown();
    ex.awaitTermination(10, TimeUnit.MINUTES);
    String errors =
        futures.stream()
            .map(
                f -> {
                  try {
                    f.get();
                    return null;
                  } catch (Exception e) {
                    log.error(e.getMessage());
                    return e.getMessage();
                  }
                })
            .filter(Objects::nonNull)
            .collect(Collectors.joining("\n"));
    if (isNotBlank(errors)) {
      throw new AssertionError(errors);
    }
  }

  /**
   * Returns tokens for each user. Alternates between sending {client-id}:{client-secret} as a
   * header, or in the request body.
   */
  @SneakyThrows
  public List<LabBotUserResult> tokens() {
    /*
     * Make necessary configuration is available.
     */
    config().baseUrl();
    config().userPassword();
    config().aud();
    config().redirectUrl();
    List<LabBotUserResult> labBotUserResultList = new CopyOnWriteArrayList<>();
    ExecutorService ex = Executors.newFixedThreadPool(10);
    List<Future<?>> futures = new ArrayList<>(userIds.size());
    for (String userId : userIds) {
      futures.add(
          ex.submit(
              () -> {
                UserCredentials userCredentials =
                    UserCredentials.builder().id(userId).password(config.userPassword()).build();
                VaOauthRobot bot =
                    makeLabBot(userCredentials, config.baseUrl(), config().credentialsMode());
                labBotUserResultList.add(
                    LabBotUserResult.builder()
                        .user(userCredentials)
                        .tokenExchange(bot.token())
                        .vaOauthRobot(bot)
                        .build());
              }));
    }
    results(ex, futures);
    return labBotUserResultList;
  }

  private static class Config {

    private Properties properties;

    @SneakyThrows
    Config(String pathname) {
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

    String aud() {
      return valueOf("va-oauth-robot.aud");
    }

    String baseUrl() {
      return valueOf("va-oauth-robot.base-url");
    }

    String clientId() {
      return valueOf("va-oauth-robot.client-id");
    }

    String clientSecret() {
      return valueOf("va-oauth-robot.client-secret");
    }

    OAuthCredentialsMode credentialsMode() {
      return OAuthCredentialsMode.valueOf(valueOf("va-oauth-robot.credentials-mode"));
    }

    OAuthCredentialsType credentialsType() {
      return OAuthCredentialsType.valueOf(valueOf("va-oauth-robot.credentials-type"));
    }

    String driver() {
      return valueOf("webdriver.chrome.driver");
    }

    boolean headless() {
      return BooleanUtils.toBoolean(valueOf("webdriver.chrome.headless"));
    }

    long navigationTimeout() {
      return NumberUtils.toLong(valueOf("va-oauth-robot.navigation-timeout-seconds", "3"), 3);
    }

    String redirectUrl() {
      return valueOf("va-oauth-robot.redirect-url");
    }

    boolean skipTwoFactorAuth() {
      return BooleanUtils.toBoolean(valueOf("va-oauth-robot.skip-two-factor-authentication"));
    }

    String state() {
      return valueOf("va-oauth-robot.state");
    }

    String userPassword() {
      return valueOf("va-oauth-robot.user-password");
    }

    private String valueOf(String name) {
      String value = properties.getProperty(name, "");
      assertThat(value).withFailMessage("System property %s must be specified.", name).isNotBlank();
      return value;
    }

    private String valueOf(String name, String defaultValue) {
      return properties.getProperty(name, defaultValue);
    }
  }

  public static class InvalidConformanceStatement extends RuntimeException {

    InvalidConformanceStatement(String message) {
      super(message);
    }
  }

  @Builder
  @Value
  public static class LabBotUserResult {

    UserCredentials user;

    TokenExchange tokenExchange;

    VaOauthRobot vaOauthRobot;

    String response;
  }

  @Value
  private static class SmartOnFhirUrls {

    String token;

    String authorize;

    /**
     * Create a new instance that will reach out to the given base URL to discover SMART on FHIR
     * information. This class will attempt to interact with /metadata endpoint of the base URL
     * immediately during construction.
     */
    @SneakyThrows
    private SmartOnFhirUrls(String baseUrl) {
      log.info("Discovering authorization endpoints from {}", baseUrl);
      String statement =
          RestAssured.given().relaxedHTTPSValidation().baseUri(baseUrl).get("metadata").asString();
      ObjectMapper mapper = new ObjectMapper();
      JsonNode node = mapper.readTree(statement);
      JsonNode oauthExtensionNode = findOauthExtensionNode(node);
      token = findTokenUri(oauthExtensionNode);
      authorize = findAuthorizeUri(oauthExtensionNode);
    }

    private static void checkConformanceStatement(boolean ok, String message) {
      if (!ok) {
        throw new InvalidConformanceStatement(message);
      }
    }

    private static <T> Optional<T> checkConformanceStatement(
        Optional<T> notNullObject, String message) {
      checkConformanceStatement(notNullObject != null, message);
      checkConformanceStatement(notNullObject.isPresent(), message);
      return notNullObject;
    }

    private static <T> T checkConformanceStatement(T notNullObject, String message) {
      checkConformanceStatement(notNullObject != null, message);
      return notNullObject;
    }

    private static String findAuthorizeUri(JsonNode jsonNode) {
      Optional<JsonNode> urlAuthorizeNode =
          checkConformanceStatement(
              getParentWith(jsonNode, "url", "authorize"),
              "Unable to find JSON node with 'url:authorize' key value pair");
      JsonNode authorizeNode =
          checkConformanceStatement(
              urlAuthorizeNode.get().path("valueUri"),
              "Unable to find JSON node with 'valueUri' path for authorize");
      return authorizeNode.textValue();
    }

    private static JsonNode findOauthExtensionNode(@NonNull JsonNode jsonNode) {
      JsonNode restNode =
          checkConformanceStatement(
              jsonNode.path("rest"), "Unable to find JSON node with 'rest' path");
      Optional<JsonNode> modeServerNode =
          checkConformanceStatement(
              getParentWith(restNode, "mode", "server"),
              "Unable to find child JSON node with 'mode:server' key:value pair");
      JsonNode securityNode =
          checkConformanceStatement(
              modeServerNode.get().path("security"),
              "Unable to find JSON node with 'security' path");
      JsonNode extensionNode =
          checkConformanceStatement(
              securityNode.path("extension"),
              "Unable to find JSON node with 'extension' path for securityNode");
      Optional<JsonNode> oauthUriNode =
          checkConformanceStatement(
              getParentWith(
                  extensionNode,
                  "url",
                  "http://fhir-registry.smarthealthit.org/StructureDefinition/oauth-uris"),
              "Unable to find JSON node with 'url:http://fhir-registry.smarthealthit.org/StructureDefinition/oauth-uris' key value pair");
      return checkConformanceStatement(
          oauthUriNode.get().path("extension"),
          "Unable to find JSON node with 'extension' path for oauthUriNode");
    }

    private static String findTokenUri(JsonNode oauthUriNode) {
      Optional<JsonNode> urlTokenNode =
          checkConformanceStatement(
              getParentWith(oauthUriNode, "url", "token"),
              "Unable to find JSON node with 'url:token' key value pair");
      JsonNode tokenNode =
          checkConformanceStatement(
              urlTokenNode.get().path("valueUri"),
              "Unable to find JSON node with 'valueUri' path for token");
      return tokenNode.textValue();
    }

    /** Get json parent node with given key value pair. * */
    private static Optional<JsonNode> getParentWith(
        JsonNode node, @NonNull String key, @NonNull String value) {
      for (JsonNode checkNode : node) {
        if (value.equals(checkNode.path(key).textValue())) {
          return Optional.of(checkNode);
        }
      }
      return Optional.empty();
    }
  }
}
