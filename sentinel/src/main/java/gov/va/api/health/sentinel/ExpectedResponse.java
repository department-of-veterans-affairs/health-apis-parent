package gov.va.api.health.sentinel;

import static io.restassured.config.LogConfig.logConfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.va.api.health.autoconfig.configuration.JacksonConfig;
import io.restassured.internal.support.Prettifier;
import io.restassured.response.Response;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * A decorator for the standard Rest Assured response that adds a little more error support, by
 * automatically logging everything if an validation occurs.
 */
@Data
@AllArgsConstructor(staticName = "of")
public class ExpectedResponse {
  /** The response from the HTTP request. */
  private final Response response;

  /** What should happen when logging failures. By default, everything will be logged. */
  private Consumer<Response> logAction;

  /** Mapper for deserializing responses. */
  private ObjectMapper mapper;

  /** The default logging action that logs everything. */
  public static Consumer<Response> logAll() {
    return r -> r.then().log().all();
  }

  /** A logging action that logs everything, but if the body is too big, it will be truncated . */
  public static Consumer<Response> logAllWithTruncatedBody(int max) {
    return r -> {
      r.then().log().status();
      r.then().log().headers();
      String body = new Prettifier().getPrettifiedBodyIfPossible(r, r.body());
      if (body.length() > max) {
        body = body.substring(0, max) + "... TRUNCATED ...";
      }
      logConfig().defaultStream().println(body);
    };
  }

  /** Create a new instance that will log all on a failure. */
  public static ExpectedResponse of(Response response) {
    return of(response, logAll(), JacksonConfig.createMapper());
  }

  public static ExpectedResponse of(Response response, Consumer<Response> logAction) {
    return of(response, logAction, JacksonConfig.createMapper());
  }

  /** Expect the HTTP status code to be the given value. */
  public ExpectedResponse expect(int statusCode) {
    try {
      response.then().statusCode(statusCode);
    } catch (AssertionError e) {
      log();
      throw e;
    }
    return this;
  }

  /** Expect the body to be JSON represented by the given type. */
  private <T> T expect(Class<T> type) {
    try {
      return mapper.readValue(response().asByteArray(), type);
    } catch (IOException e) {
      log();
      throw new AssertionError("Failed to parse JSON body", e);
    }
  }

  /** Expect the body to be a JSON list of the given type. */
  public <T> List<T> expectListOf(Class<T> type) {
    try {
      return mapper.readValue(
          response().asByteArray(),
          mapper.getTypeFactory().constructCollectionType(List.class, type));
    } catch (IOException e) {
      log();
      throw new AssertionError("Failed to parse JSON body", e);
    }
  }

  /** Expect the body to be JSON of the given type, then perform Java Bean validation against it. */
  public <T> T expectValid(Class<T> type) {
    T payload = expect(type);
    Set<ConstraintViolation<T>> violations =
        Validation.buildDefaultValidatorFactory().getValidator().validate(payload);
    if (violations.isEmpty()) {
      return payload;
    }
    log();
    StringBuilder message = new StringBuilder("Constraint Violations:");
    violations.forEach(
        v ->
            message
                .append('\n')
                .append(v.getMessage())
                .append(": ")
                .append(v.getPropertyPath().toString())
                .append(" = ")
                .append(v.getInvalidValue()));
    message.append("\n\nDetails:");
    violations.forEach(v -> message.append('\n').append(v));
    throw new AssertionError(message.toString());
  }

  @SuppressWarnings("UnusedReturnValue")
  ExpectedResponse log() {
    logAction.accept(response());
    return this;
  }
}
