package gov.va.api.health.autoconfig.logging;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.Delegate;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * This aspect is used to automatically log entry and exit of Controller methods that are annotated
 * with Loggable or GetRequest.
 */
@Aspect
@Component
public class MethodExecutionLogger {

  /**
   * Some state is shared by loggable methods in the same thread. This is used to track the IDs,
   * loggable stack level, etc. See the Context below that is responsible for initializing the value
   * per thread if it is not already set.
   */
  private final ThreadLocal<SharedState> sharedState = new ThreadLocal<>();

  /** Log enter and leave messages based on the presence of Loggable or GetMapping annotations. */
  @Around(
      "within(@gov.va.api.health.autoconfig.logging.Loggable *)"
          + "|| (execution(* *(..))"
          + "  && (@annotation(gov.va.api.health.autoconfig.logging.Loggable)"
          + "    || @annotation(org.springframework.web.bind.annotation.GetMapping)"
          + "    || @annotation(org.springframework.web.bind.annotation.PostMapping)))")
  public Object log(ProceedingJoinPoint point) throws Throwable {
    try (Context context = new Context(point)) {
      if (context.logStart()) {
        context
            .log()
            .info(
                "ENTER {} {} {} {} {}",
                context.id(),
                context.level(),
                context.method().getName(),
                context.argumentsAsString(),
                context.requestUri().replaceAll("[\r\n]", ""));
      }
      Throwable thrown = null;
      try {
        return point.proceed();
      } catch (Throwable oops) {
        thrown = oops;
        throw oops;
      } finally {
        if (context.logEnd()) {
          context
              .log()
              .info(
                  "LEAVE {} {} {} {} ms {} {}",
                  context.id(),
                  context.level(),
                  context.method().getName(),
                  context.markTiming(),
                  context.timingSummary(),
                  context.exceptionAsString(thrown));
        }
      }
    }
  }

  @Getter
  private static class SharedState {

    private final String id;

    private int level;

    private List<String> timings;

    SharedState() {
      id = String.format("%6X", System.currentTimeMillis() & 0xFFFFFF);
      level = 1;
      timings = new LinkedList<>();
    }

    void levelDown() {
      level -= 1;
    }

    void levelUp() {
      level += 1;
    }
  }

  /**
   * The loggable context maintains information about the current loggable method. It provides
   * automatic use or initialization of context ID and level using thread local state variables.
   * Context instances must be closed after creation for proper management.
   */
  @Value
  private class Context implements AutoCloseable {

    ProceedingJoinPoint point;

    long start;

    Logger log;

    Method method;

    Loggable annotation;

    boolean startOfLoggingChain;

    @Delegate SharedState state;

    HttpServletRequest request;

    /**
     * Create a new context extracting information from the point. This context will use or set it's
     * ID and level from ThreadLocals. Context's must be closed to clean up ID and depth.
     */
    Context(ProceedingJoinPoint point) {
      this.point = point;
      start = System.currentTimeMillis();
      log = LoggerFactory.getLogger(point.getSignature().getDeclaringType());
      method = MethodSignature.class.cast(point.getSignature()).getMethod();
      annotation = method.getAnnotation(Loggable.class);
      request =
          ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
      /*
       * The ID and level need to be determined based on the thread. The ID and previous level may
       * have already be set by an earlier loggable method or this could the be first. If available,
       * we want to use it. If not, we need to create it and then later clean up.
       */
      SharedState existingId = sharedState.get();
      if (existingId == null) {
        state = new SharedState();
        startOfLoggingChain = true;
        sharedState.set(state);
      } else {
        state = existingId;
        startOfLoggingChain = false;
        state.levelUp();
      }
    }

    /** If method arguments are enabled, return them. Otherwise return an empty string. */
    String argumentsAsString() {
      return logArguments() ? Arrays.toString(point.getArgs()) : "";
    }

    /**
     * If the ID was created by this context, remove it from the thread and reset the depth.
     * Otherwise, decrement the depth only.
     */
    @Override
    public void close() {
      if (startOfLoggingChain) {
        sharedState.remove();
      } else {
        state.levelDown();
      }
    }

    /**
     * If exceptions are enabled and thrown is set, convert it to a simple string. Otherwise return
     * empty.
     */
    String exceptionAsString(Throwable thrown) {
      return thrown != null && logException() ? thrown.getClass().getSimpleName() : "";
    }

    /** Return true if method arguments should be logged. */
    boolean logArguments() {
      return log.isInfoEnabled() && (annotation == null || annotation.arguments());
    }

    /** Return true if end of invocation should be logged. */
    boolean logEnd() {
      return log.isInfoEnabled() && (annotation == null || annotation.leave());
    }

    /** Return true if exception summary should be logged. */
    boolean logException() {
      return log.isInfoEnabled() && (annotation == null || annotation.exception());
    }

    /** Return true if start of invocation should be logged. */
    boolean logStart() {
      return log.isInfoEnabled() && (annotation == null || annotation.enter());
    }

    /** Return how long has this been running. */
    long markTiming() {
      long elapsed = System.currentTimeMillis() - start;
      state.timings().add(method.getName() + " " + elapsed);
      return elapsed;
    }

    /** Get the request URI for logging. */
    String requestUri() {
      // We don't want this printed every time we ENTER somewhere. Do it only on the top level.
      if (state.level() != 1) {
        return "";
      }
      String uri = request.getRequestURI();
      String query = request.getQueryString();

      return query == null ? uri : uri + "?" + query;
    }

    /**
     * The assumption is that `markTiming` is called before this method. The timing summary is only
     * available for the top of the loggable stack. It is also possible that the loggable stack only
     * contained the top. So summary must also have at least one other entry.
     */
    String timingSummary() {
      if (!startOfLoggingChain || state.timings().size() < 2) {
        return "";
      }
      return "["
          + state.timings().subList(0, state.timings().size() - 1).stream()
              .collect(Collectors.joining(","))
          + "]";
    }
  }
}
