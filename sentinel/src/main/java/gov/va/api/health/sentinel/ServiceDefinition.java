package gov.va.api.health.sentinel;

import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/** Defines particulars for interacting with a specific service. */
@Value
@Builder
@AllArgsConstructor
public final class ServiceDefinition {

  String url;

  int port;

  @NonNull String apiPath;

  Supplier<Optional<String>> accessToken;

  /** Returns Request Specification. */
  public RequestSpecification requestSpecification() {
    RequestSpecification spec =
        RestAssured.given().baseUri(url()).port(port()).relaxedHTTPSValidation();

    Optional<String> token = accessToken.get();
    if (token.isPresent()) {
      spec = spec.header("Authorization", "Bearer " + token.get());
    }
    return spec;
  }

  /**
   * Guaranteed to return a url + path that adds / as necessary to produce a url that ends in a /.
   * e.g. https://something.com/my/cool/api/
   */
  public String urlWithApiPath() {
    StringBuilder builder = new StringBuilder(url());
    if (!apiPath().startsWith("/")) {
      builder.append('/');
    }
    builder.append(apiPath());
    if (!apiPath.endsWith("/")) {
      builder.append('/');
    }
    return builder.toString();
  }
}
