package gov.va.api.health.autoconfig.logging;

import static gov.va.api.health.autoconfig.logging.LogSanitizer.sanitize;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.va.api.health.autoconfig.configuration.JacksonConfig;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.experimental.Delegate;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestParam;
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
  private static final ThreadLocal<SharedState> sharedState = new ThreadLocal<>();

  /** Log enter and leave messages based on the presence of Loggable or GetMapping annotations. */
  @Around(
      "within(@gov.va.api.health.autoconfig.logging.Loggable *)"
          + "|| (execution(* *(..))"
          + "  && (@annotation(gov.va.api.health.autoconfig.logging.Loggable)"
          + "    || @annotation(org.springframework.web.bind.annotation.GetMapping)"
          + "    || @annotation(org.springframework.web.bind.annotation.PostMapping)))")
  public Object log(ProceedingJoinPoint point) throws Throwable {
    try (Context context = new Context(point)) {
      LogEntry entry = LogEntry.create(context);
      if (context.logStart()) {
        context.log("ENTER {}", entry);
      }
      try {
        return point.proceed();
      } catch (Throwable thrown) {
        entry.exception(context.exceptionTypeAsString(thrown));
        entry.message(context.exceptionMessageAsString(thrown));
        context.markError();
        throw thrown;
      } finally {
        context.markTiming();
        if (context.logEnd()) {
          entry.timing(context.timingSummary());
          context.log("LEAVE {}", entry);
        }
      }
    }
  }

  /**
   * The loggable context maintains information about the current loggable method. It provides
   * automatic use or initialization of context ID and level using thread local state variables.
   * Context instances must be closed after creation for proper management.
   */
  @Value
  private static class Context implements AutoCloseable {
    ProceedingJoinPoint point;

    long start;

    Logger logger;

    Class<?> declaringType;

    Method method;

    Loggable annotation;

    boolean startOfLoggingChain;

    @Delegate SharedState state;

    Optional<HttpServletRequest> request;

    /** Parameter index to HTTP request parameter name. */
    Map<Integer, String> redactedParameters;

    /**
     * Create a new context extracting information from the point. This context will use or set it's
     * ID and level from ThreadLocals. Context's must be closed to clean up ID and depth.
     */
    Context(ProceedingJoinPoint point) {
      this.point = point;
      start = System.currentTimeMillis();
      declaringType = point.getSignature().getDeclaringType();
      logger = LoggerFactory.getLogger(declaringType);
      method = ((MethodSignature) point.getSignature()).getMethod();
      annotation = method.getAnnotation(Loggable.class);
      HttpServletRequest maybeRequest = null;
      try {
        maybeRequest =
            ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();
      } catch (Exception e) {
        // Could not find request, probably on a parallel stream thread.
      }
      request = Optional.ofNullable(maybeRequest);
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
      redactedParameters = determineRedactedParameters();
    }

    /** If method arguments are enabled, return them. Otherwise return an empty string. */
    String argumentsAsString() {
      return logArguments() ? String.join(",", getPrintableArguments()) : "";
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
     * Build a mapping from parameter position to request parameter name that should be redacted.
     */
    private Map<Integer, String> determineRedactedParameters() {
      Parameter[] parameters = method.getParameters();
      Map<Integer, String> redacted = new HashMap<>(parameters.length);
      int index = 0;
      for (Parameter p : parameters) {
        if (p.getAnnotation(Redact.class) != null) {
          var requestParam = p.getAnnotation(RequestParam.class);
          String name = null;
          if (requestParam != null) {
            name = requestParam.value();
            if (isBlank(name)) {
              name = requestParam.name();
            }
          }
          if (isBlank(name)) {
            name = p.getName();
          }
          redacted.put(index, name);
        }
        index++;
      }
      return redacted;
    }

    /**
     * If exceptions are enabled and thrown is set, convert it to a simple string. Otherwise return
     * empty.
     */
    String exceptionMessageAsString(Throwable thrown) {
      return thrown != null && logException() ? sanitize(thrown.getMessage()) : "";
    }

    /**
     * If exceptions are enabled and thrown is set, convert it to a simple string. Otherwise return
     * empty.
     */
    String exceptionTypeAsString(Throwable thrown) {
      return thrown != null && logException() ? thrown.getClass().getSimpleName() : "";
    }

    private List<String> getPrintableArguments() {
      Object[] arguments = point.getArgs();
      List<String> printableArguments = new ArrayList<>(arguments.length);
      for (int i = 0; i < arguments.length; i++) {
        printableArguments.add(
            redactedParameters.containsKey(i) ? "***" : String.valueOf(arguments[i]));
      }
      return printableArguments;
    }

    /** Log, or defer logging the message. This uses SLF4J logger semantics. */
    void log(String message, Object... args) {
      if (startOfLoggingChain) {
        if (state().error()) {
          state.deferredLogs().forEach(DeferredLog::logNow);
        }
        logger().info(message, args);
      } else {
        state().deferredLogs().add(new DeferredLog(logger(), message, args));
      }
    }

    /** Return true if method arguments should be logged. */
    boolean logArguments() {
      return logger.isInfoEnabled() && (annotation == null || annotation.arguments());
    }

    /** Return true if end of invocation should be logged. */
    boolean logEnd() {
      return logger.isInfoEnabled() && (annotation == null || annotation.leave());
    }

    /** Return true if exception summary should be logged. */
    boolean logException() {
      return logger.isInfoEnabled() && (annotation == null || annotation.exception());
    }

    /** Return true if start of invocation should be logged. */
    boolean logStart() {
      return logger.isInfoEnabled() && (annotation == null || annotation.enter());
    }

    /** Indicate an error has occurred, which will trigger additional logging. */
    void markError() {
      state.error(true);
    }

    /** Return how long has this been running. */
    void markTiming() {
      long elapsed = System.currentTimeMillis() - start;
      state.timings().addFirst(method.getName() + " " + elapsed);
    }

    /** Get the request URI for logging. */
    String requestUri() {
      // We don't want this printed every time we ENTER somewhere. Do it only on the top level.
      if (request.isEmpty() || state.level() != 1) {
        return "";
      }
      String uri = request.get().getRequestURI();

      String query;
      if (redactedParameters.isEmpty()) {
        query = request.get().getQueryString();
      } else {
        var redacted = new HashSet<>(redactedParameters.values());
        var printableRequestParams = new ArrayList<String>();
        var requestParams = request.get().getParameterMap();
        requestParams.forEach(
            (name, values) -> {
              for (var value : values) {
                printableRequestParams.add(name + "=" + (redacted.contains(name) ? "***" : value));
              }
            });
        query = String.join(",", printableRequestParams);
      }
      return query == null ? uri : uri + "?" + query;
    }

    /**
     * The assumption is that `markTiming` is called before this method. The timing summary is only
     * available for the top of the loggable stack. It is also possible that the loggable stack only
     * contained the top. So summary must also have at least one other entry.
     */
    String timingSummary() {
      return String.join(",", state.timings());
    }
  }

  /**
   * Statements that are not the start of the thread (level 2 and beyond) will be deferred and only
   * printed if an error occurs.
   */
  @Value
  @AllArgsConstructor
  private static class DeferredLog {
    Logger logger;

    String message;

    Object[] args;

    void logNow() {
      logger.info(message, args);
    }
  }

  @Data
  @JsonPropertyOrder({"id", "level", "method", "request", "timing", "exception", "message"})
  private static class LogEntry {
    private static final ObjectMapper MAPPER = JacksonConfig.createMapper();

    String id;

    String method;

    String timing;

    String request;

    String exception;

    String message;

    int level;

    /** Create a new instance harvesting information from the context. */
    private static LogEntry create(Context context) {
      LogEntry entry =
          new LogEntry()
              .id(context.id())
              .level(context.level())
              .method(
                  context.declaringType().getSimpleName()
                      + "."
                      + context.method().getName()
                      + "("
                      + context.argumentsAsString()
                      + ")");
      if (context.startOfLoggingChain()) {
        entry.request(context.requestUri());
      }
      return entry;
    }

    @Override
    @SneakyThrows
    public String toString() {
      return MAPPER.writeValueAsString(this);
    }
  }

  @Getter
  private static class SharedState {
    private final String id;

    private final Deque<String> timings;

    private final List<DeferredLog> deferredLogs;

    private int level;

    @Setter private boolean error;

    SharedState() {
      id = String.format("%6X", System.currentTimeMillis() & 0xFFFFFF);
      level = 1;
      timings = new ArrayDeque<>();
      deferredLogs = new ArrayList<>();
    }

    void levelDown() {
      level -= 1;
    }

    void levelUp() {
      level += 1;
    }
  }
}
