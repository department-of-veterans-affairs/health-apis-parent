package gov.va.api.health.sentinel;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.Value;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.web.client.RestTemplate;

@Builder
public class SystemOauthRobot {
  @Getter @NonNull private final Configuration config;

  /* setIssuedAt() and setExpiration() both only support java.util.Date as input. */
  @SuppressWarnings("JdkObsolete")
  private String clientAssertion() {
    Key key = Keys.hmacShaKeyFor(config().clientSecret().getBytes(StandardCharsets.UTF_8));
    return Jwts.builder()
        .setHeaderParam("typ", "JWT")
        .setAudience(config().audience())
        .setIssuer(config().clientId())
        .setSubject(config().clientId())
        .setId(UUID.randomUUID().toString())
        .setIssuedAt(Date.from(Instant.now()))
        .setExpiration(Date.from(config().expiration()))
        .signWith(key, SignatureAlgorithm.HS256)
        .compact();
  }

  private String requestBody() {
    StringBuilder b = new StringBuilder();
    b.append("grant_type=").append("client_credentials");
    b.append("&client_assertion_type=")
        .append("urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
    b.append("&client_assertion=").append(clientAssertion());
    b.append("&scope=").append(String.join(" ", config().scopes()));
    return b.toString();
  }

  /** Exchange for a System oauth token. */
  @SneakyThrows
  public TokenExchange token() {
    RequestEntity<String> request =
        RequestEntity.post(new URI(config().tokenUrl()))
            .contentType(MediaType.parseMediaType("application/x-www-form-urlencoded"))
            .accept(MediaType.parseMediaType("application/json"))
            .body(requestBody());
    RestTemplate rt = new RestTemplate();
    return rt.exchange(request, TokenExchange.class).getBody();
  }

  @Value
  @Builder
  public static class Configuration {
    @NonNull String tokenUrl;

    @NonNull String clientId;

    @NonNull String clientSecret;

    @NonNull String audience;

    @NonNull Set<String> scopes;

    // Default expiration to 30 minutes
    @Builder.Default Instant expiration = Instant.now().plusSeconds(60 * 30);
  }
}
