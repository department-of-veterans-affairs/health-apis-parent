package gov.va.api.health.sentinel;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.http.Method;
import io.restassured.response.Response;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

/**
 * The FhirTestClient bakes in some functionality such as making the requests with all three
 * supported content types: application/json, application/fhir+json, and application/json+fhir. When
 * using this test client, callers will need to furnish an ErrorsAreFunctionallyEqual that will
 * determine whether each of the three content types returns a functionally equivalent (ignoring
 * transient data like timestamps and sequence numbers) response.
 */
@Slf4j
@Value
@Builder
public final class FhirTestClient implements TestClient {
  ServiceDefinition service;

  ExecutorService executorService =
      Executors.newFixedThreadPool(
          SentinelProperties.threadCount(
              "sentinel.threads", Runtime.getRuntime().availableProcessors()));

  @NonNull ErrorsAreFunctionallyEqual errorResponseEqualityCheck;

  Supplier<ObjectMapper> mapper;

  @Builder.Default
  List<String> contentTypes = List.of("application/fhir+json", "application/json+fhir");

  @Override
  public ExpectedResponse get(String path, String... params) {
    return get(null, path, params);
  }

  @Override
  @SneakyThrows
  public ExpectedResponse get(Map<String, String> headers, String path, String... params) {
    Future<Response> baselineResponseFuture =
        executorService.submit(
            () -> {
              return get(headers, "application/json", path, params);
            });

    if (path.startsWith("/actuator")) {
      /* Health checks, metrics, etc. do not have FHIR compliance requirements */
      return ExpectedResponse.of(baselineResponseFuture.get(5, TimeUnit.MINUTES));
    }

    // We dont want to run requests twice.
    List<Future<Response>> crystalBall =
        contentTypes.stream()
            .distinct()
            .filter(s -> !s.equals("application/json"))
            .map(
                contentType ->
                    executorService.submit(
                        () -> {
                          return get(headers, contentType, path, params);
                        }))
            .collect(Collectors.toList());

    final Response baselineResponse = baselineResponseFuture.get(5, TimeUnit.MINUTES);

    for (Future<Response> future : crystalBall) {
      Response fhirResponse = future.get(5, TimeUnit.MINUTES);

      assertThat(fhirResponse.getStatusCode())
          .withFailMessage(
              "status: application/json ("
                  + baselineResponse.getStatusCode()
                  + ") does not equal "
                  + fhirResponse.contentType()
                  + "("
                  + fhirResponse.getStatusCode()
                  + ")")
          .isEqualTo(baselineResponse.getStatusCode());

      if (baselineResponse.getStatusCode() >= 400) {
        /*
         * Error responses must be returned as OOs but
         * contains timestamps that prevents direct comparison
         */
        assertThat(errorResponseEqualityCheck.equals(baselineResponse.body(), fhirResponse.body()))
            .isTrue();
      } else {
        // OK responses
        assertThat(baselineResponse.body().asString()).isEqualTo(fhirResponse.body().asString());
      }
    }
    return ExpectedResponse.of(baselineResponse);
  }

  private Response get(
      Map<String, String> maybeHeaders, String contentType, String path, Object[] params) {
    Response response = null;

    // We'll make the request at least one time and as many as maxAttempts if we get a 500 error.
    final int maxAttempts = 3;
    for (int i = 0; i < maxAttempts; i++) {
      if (i > 0) {
        log.info("Making retry attempt {} for {}:{} after failure.", i, contentType, path);
      }

      response =
          service()
              .requestSpecification()
              .contentType(contentType)
              .accept(contentType)
              .headers(maybeHeaders == null ? Collections.emptyMap() : maybeHeaders)
              .request(Method.GET, path, params);

      if (response.getStatusCode() != 500) {
        return response;
      }
    }

    return response;
  }

  @Override
  public ExpectedResponse post(String path, Object body) {
    return post(
        Map.of("Content-Type", "application/fhir+json", "Accept", "application/fhir+json"),
        path,
        body);
  }

  @Override
  public ExpectedResponse post(Map<String, String> headers, String path, Object body) {
    return ExpectedResponse.of(
        service().requestSpecification().headers(headers).body(body).request(Method.POST, path));
  }
}
