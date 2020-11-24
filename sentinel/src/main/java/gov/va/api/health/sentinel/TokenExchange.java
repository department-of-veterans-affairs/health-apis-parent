package gov.va.api.health.sentinel;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class TokenExchange {
  @JsonProperty("error")
  String error;

  @JsonProperty("error_description")
  String errorDescription;

  @JsonProperty("access_token")
  String accessToken;

  @JsonProperty("token_type")
  String tokenType;

  @JsonProperty("expires_at")
  long expiresAt;

  @JsonProperty("expires_in")
  long expiresIn;

  @JsonProperty("scope")
  String scope;

  @JsonProperty("id_token")
  String idToken;

  @JsonProperty("patient")
  String patient;

  @JsonProperty("state")
  String state;

  @JsonProperty("refresh_token")
  String refreshToken;

  public boolean isError() {
    return isNotBlank(error) || isNotBlank(errorDescription);
  }
}
