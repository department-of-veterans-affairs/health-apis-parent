package gov.va.api.health.autoconfig.logging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** If placed in front of a method argument, it will not be logged. */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.PARAMETER})
public @interface Redact {
  /** If true the argument will not be logged. */
  boolean value() default true;
}
