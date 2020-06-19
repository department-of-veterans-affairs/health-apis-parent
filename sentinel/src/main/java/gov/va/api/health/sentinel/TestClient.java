package gov.va.api.health.sentinel;

import java.util.Map;

/**
 * The TestClient provides an abstraction to REST requests for a server that returns Rest Assured
 * responses decorated with easy to assert expectations.
 */
public interface TestClient {
  /**
   * Perform a get request with optional path params.
   *
   * <pre>
   *   tc.get("api/v1/awesome");
   *   tc.get("api/v1/awesome/{animal}/{id}","possum","harvey");
   * </pre>
   */
  ExpectedResponse get(String path, String... params);

  /**
   * Perform a get request with headers and optional path params.
   *
   * <pre>
   *   Map&lt;String,String> headers = ImmutableMap.of("CLIENT_ID","#1p055umf@n");
   *   tc.get(headers, "api/v1/awesome");
   *   tc.get(headers, "api/v1/awesome/{animal}/{id}","possum","harvey");
   * </pre>
   */
  ExpectedResponse get(Map<String, String> headers, String path, String... params);

  /** Perform a post request with the given body. */
  ExpectedResponse post(String path, Object body);

  ExpectedResponse post(Map<String, String> headers, String path, Object body);

  ServiceDefinition service();
}
