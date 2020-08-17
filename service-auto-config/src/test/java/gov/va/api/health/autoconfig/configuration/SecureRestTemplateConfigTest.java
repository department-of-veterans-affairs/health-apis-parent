package gov.va.api.health.autoconfig.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import gov.va.api.health.autoconfig.configuration.SecureRestTemplateConfig.FailedToConfigureSsl;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

@Slf4j
public class SecureRestTemplateConfigTest {
  @Test
  @SneakyThrows
  public void errorAreLogged() {
    /* Would throw null pointers if the key store was attempted to be used. */
    RestTemplate rt = makeOne(SslClientProperties.builder().enableClient(false).build());
    try {
      rt.getForEntity("http://example.com/404", String.class);
    } catch (Exception e) {
      /* This is completely expected. */
    }
  }

  @Test(expected = FailedToConfigureSsl.class)
  @SneakyThrows
  public void exceptionIsThrownWhenClientKeyIsWrong() {
    tryWebRequest(
        makeOne(
            SslClientProperties.builder()
                .enableClient(true)
                .verify(true)
                .clientKeyPassword("nope")
                .keyStore("classpath:test-keystore.jks")
                .keyStorePassword("secret")
                .useTrustStore(true)
                .trustStore("classpath:test-truststore.jks")
                .trustStorePassword("secret")
                .build()));
  }

  @Test(expected = FailedToConfigureSsl.class)
  @SneakyThrows
  public void exceptionIsThrownWhenKeyStoreCannotBeFound() {
    makeOne(
        SslClientProperties.builder()
            .enableClient(true)
            .verify(true)
            .clientKeyPassword("secret")
            .keyStore("classpath:nope")
            .keyStorePassword("secret")
            .useTrustStore(true)
            .trustStore("classpath:test-truststore.jks")
            .trustStorePassword("secret")
            .build());
  }

  @Test(expected = FailedToConfigureSsl.class)
  @SneakyThrows
  public void exceptionIsThrownWhenKeyStoreCannotBeOpened() {
    makeOne(
        SslClientProperties.builder()
            .enableClient(true)
            .verify(true)
            .clientKeyPassword("secret")
            .keyStore("classpath:corrupt-keystore.jks")
            .keyStorePassword("secret")
            .useTrustStore(true)
            .trustStore("classpath:test-truststore.jks")
            .trustStorePassword("wrong")
            .build());
  }

  @Test(expected = IllegalArgumentException.class)
  @SneakyThrows
  public void exceptionIsThrownWhenKeyStoreIsNotFileOrClasspath() {
    makeOne(
        SslClientProperties.builder()
            .enableClient(true)
            .verify(true)
            .clientKeyPassword("secret")
            .keyStore("http://nope.com")
            .keyStorePassword("secret")
            .useTrustStore(true)
            .trustStore("classpath:test-truststore.jks")
            .trustStorePassword("secret")
            .build());
  }

  @Test(expected = FailedToConfigureSsl.class)
  @SneakyThrows
  public void exceptionIsThrownWhenKeyStorePasswordIsWrong() {
    makeOne(
        SslClientProperties.builder()
            .enableClient(true)
            .verify(true)
            .clientKeyPassword("secret")
            .keyStore("classpath:test-keystore.jks")
            .keyStorePassword("secret")
            .useTrustStore(true)
            .trustStore("classpath:test-truststore.jks")
            .trustStorePassword("wrong")
            .build());
  }

  @Test
  @SneakyThrows
  public void keyStoreAndTrustStoresAreUsedWhenEnabled() {
    tryWebRequest(
        makeOne(
            SslClientProperties.builder()
                .enableClient(true)
                .verify(true)
                .clientKeyPassword("secret")
                .keyStore("classpath:test-keystore.jks")
                .keyStorePassword("secret")
                .useTrustStore(true)
                .trustStore("classpath:test-truststore.jks")
                .trustStorePassword("secret")
                .build()));
  }

  @Test
  @SneakyThrows
  public void keyStoreAreUsedWhenEnabledButTrustStoreDisabled() {
    /* Would throw null pointers if the key store was attempted to be used. */
    tryWebRequest(
        makeOne(
            SslClientProperties.builder()
                .enableClient(true)
                .verify(false)
                .clientKeyPassword("secret")
                .keyStore("classpath:test-keystore.jks")
                .keyStorePassword("secret")
                .useTrustStore(false)
                .build()));
  }

  @Test
  @SneakyThrows
  public void keyStoresAreIgnoredWhenSslIsDisabled() {
    /* Would throw null pointers if the key store was attempted to be used. */
    tryWebRequest(makeOne(SslClientProperties.builder().enableClient(false).build()));
  }

  private RestTemplate makeOne(SslClientProperties props) {
    assertThat(props.equals(new SslClientProperties())).isFalse();
    assertThat(props.hashCode()).isNotEqualTo(1);
    RestTemplateBuilder rtb = new RestTemplateBuilder();
    return new SecureRestTemplateConfig(props).restTemplate(rtb);
  }

  /**
   * Since we're using external website, example.com, we don't want to rely too heavily on it being
   * available. The risk is low with the SecureRestTemplateConfig class to begin with, so if we
   * can't exercise this portion we'll be ok. Integration tests later on will also be exercising it.
   */
  private void tryWebRequest(RestTemplate rt) {
    try {
      rt.getForEntity("http://example.com", String.class);
      rt.getForEntity("https://example.com", String.class);
    } catch (Exception e) {
      log.warn(
          "Could not completely test {}: {}",
          SecureRestTemplateConfig.class.getSimpleName(),
          e.getMessage());
    }
  }
}
