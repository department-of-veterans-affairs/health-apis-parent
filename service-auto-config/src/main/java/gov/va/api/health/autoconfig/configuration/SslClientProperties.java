package gov.va.api.health.autoconfig.configuration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Application properties for SSL client configuration.
 *
 * <pre>
 * ssl.enable-client=true
 * ssl.key-store=file:certs/system/DVP-DVP-NONPROD.jks
 * ssl.key-store-password=secret
 * ssl.client-key-password=secret
 * ssl.use-trust-store=true
 * ssl.trust-store=file:certs/system/DVP-NONPROD-truststore.jks
 * ssl.trust-store-password=secret
 * </pre>
 */
@SuppressWarnings("DefaultAnnotationParam")
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties("ssl")
@Data
@Accessors(fluent = false)
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class SslClientProperties {

  @Builder.Default private boolean enableClient = true;
  @Builder.Default private boolean verify = true;
  private String loggingEncryptionKey;
  private String keyStore;
  private String keyStorePassword;
  private String clientKeyPassword;
  private boolean useTrustStore;
  private String trustStore;
  private String trustStorePassword;

  char[] clientKeyPassword() {
    return getClientKeyPassword().toCharArray();
  }

  char[] keyStorePassword() {
    return getKeyStorePassword().toCharArray();
  }

  char[] trustStorePassword() {
    return getTrustStorePassword().toCharArray();
  }
}
