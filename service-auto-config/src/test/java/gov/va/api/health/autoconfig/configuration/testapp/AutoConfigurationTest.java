package gov.va.api.health.autoconfig.configuration.testapp;

import gov.va.api.health.autoconfig.configuration.JacksonConfig;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import({FugaziApplication.class, JacksonConfig.class})
@TestPropertySource(properties = {"ssl.enable-client=false"})
@Slf4j
public class AutoConfigurationTest {
  @Autowired TestRestTemplate rest;

  @Test
  public void boom() {
    Assertions.assertThrows(
        RuntimeException.class, () -> rest.getForEntity("/boom?kaboom=kapow", Fugazi.class));
  }

  @Test
  public void jacksonIsEnabled() {
    log.info("{}", Fugazi.FugaziBuilder.class.getName());
    log.info("{}", rest.getForEntity("/hello", Fugazi.class));
  }
}
