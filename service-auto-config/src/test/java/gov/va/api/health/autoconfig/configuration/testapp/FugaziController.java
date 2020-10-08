package gov.va.api.health.autoconfig.configuration.testapp;

import gov.va.api.health.autoconfig.configuration.testapp.Fugazi.CustomBuilder;
import gov.va.api.health.autoconfig.configuration.testapp.Fugazi.Specified;
import gov.va.api.health.autoconfig.logging.Redact;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SuppressWarnings("WeakerAccess")
@RestController
public class FugaziController {

  @Autowired FugaziComponent fuz;

  @GetMapping(path = "/boom")
  public Fugazi boom(
      @RequestParam(name = "kaboom", defaultValue = "nope", required = false) String kaboom) {
    throw new RuntimeException("FUGAZI " + fuz.now() + " " + kaboom);
  }

  @GetMapping(path = "/hello")
  public Fugazi hello() {
    return Fugazi.builder()
        .thing("Howdy")
        .time(fuz.now())
        .specified(Specified.builder().troofs(true).build())
        .cb(CustomBuilder.makeOne().one(1).build())
        .build();
  }

  @GetMapping(path = "/say-hi")
  public String sayHi(
      @RequestParam("name") String name,
      @Redact @RequestParam("secret") String secret,
      @Redact @RequestParam String[] alsoSecret) {
    return fuz.someSecrets(name, secret);
  }
}
