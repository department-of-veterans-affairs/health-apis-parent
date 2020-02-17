package gov.va.api.health.autoconfig.configuration.testapp;

import gov.va.api.health.autoconfig.configuration.JacksonConfig;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import({FugaziApplication.class, JacksonConfig.class})
@TestPropertySource(properties = {"ssl.enable-client=false"})
@Slf4j
public class AutoConfigurationTest {
  @Autowired TestRestTemplate rest;

  @Test(expected = RuntimeException.class)
  public void boom() {
    ResponseEntity<Fugazi> f = rest.getForEntity("/hello", Fugazi.class);
    rest.getForEntity("/boom?kaboom=kapow", Fugazi.class);
  }

  @Test
  public void jacksonIsEnabled() {
    log.info("{}", Fugazi.FugaziBuilder.class.getName());
    ResponseEntity<Fugazi> f = rest.getForEntity("/hello", Fugazi.class);
    log.info("{}", f);
  }
}
