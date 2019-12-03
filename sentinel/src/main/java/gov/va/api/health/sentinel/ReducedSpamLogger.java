package gov.va.api.health.sentinel;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Supplier;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Delegate;
import org.slf4j.Logger;
import org.slf4j.helpers.MessageFormatter;

/**
 * This specialized implementation of the SLF4J logger adds "Once" methods that will only log
 * messages one time to reduce the amount of spam generated in testing log files.
 */
@Value
@Builder
public class ReducedSpamLogger implements Logger {

  @Delegate Logger logger;

  Set<String> alreadyLogged = new CopyOnWriteArraySet<>();

  /** Log this message exactly once, performing SLF4J message substitution. */
  public void infoOnce(String message, Object... args) {
    infoOnce(() -> MessageFormatter.arrayFormat(message, args).getMessage());
  }

  /** Log the message eactly once. */
  public void infoOnce(String message) {
    infoOnce(() -> message);
  }

  /** Log the message supplied exactly once. */
  public void infoOnce(Supplier<String> messageSupplier) {
    if (!logger.isInfoEnabled()) {
      return;
    }
    String message = messageSupplier.get();
    if (alreadyLogged.add(message)) {
      logger.info(message);
    }
  }
}
