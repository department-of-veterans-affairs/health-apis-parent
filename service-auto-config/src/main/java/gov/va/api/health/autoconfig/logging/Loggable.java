package gov.va.api.health.autoconfig.logging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation can be added to methods of Spring components to have entry and exit automatically
 * logged. Note that RestController methods with GetRequest or PostRequest will automatically be
 * logged and do not need this annotation specifically.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Loggable {
  /** Log arguments in ENTER messages. */
         boolean arguments() default true;

  /** Log ENTER messages. */
  boolean enter() default true;

  /** Log exception class and message in LEAVE messages. */
  boolean exception() default true;

  /** Log LEAVE messages. */
  boolean leave() default true;
}
