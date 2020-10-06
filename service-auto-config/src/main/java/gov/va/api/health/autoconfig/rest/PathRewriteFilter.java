package gov.va.api.health.autoconfig.rest;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.Builder;
import lombok.Singular;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * This filter can be used to strip leading path elements from requests, allow the application to
 * service different endpoints on a load balancer without having to specify each path on every
 * controller.
 *
 * <p>To use, create a Configuration object that produces this filter as a Bean.
 *
 * <pre>
 * &amp;Configuration
 * public class PathRewriteConfig {
 *   &amp;Bean
 *   FilterRegistrationBean&lt;PathRewriteFilter> patientRegistrationFilter() {
 *     var filter =
 *         PathRewriteFilter.builder()
 *           .removeLeadingPath(List.of("/awesome/", "/neato/bandito/"))
 *           .build();
 *     var registration = new FilterRegistrationBean&lt;PathRewriteFilter>();
 *     registration.setFilter(filter);
 *     registration.addUrlPatterns(filter.removeLeadingPathsAsUrlPatterns());
 *     return registration;
 *   }
 * }
 * </pre>
 */
public class PathRewriteFilter extends OncePerRequestFilter {

  /** List of paths that must end with "/". */
  private final List<String> removeLeadingPath;

  /** Create a new instance that enforces paths end with a /. */
  @Builder
  public PathRewriteFilter(@Singular("removeLeadingPath") List<String> removeLeadingPath) {
    this.removeLeadingPath = removeLeadingPath == null ? List.of() : removeLeadingPath;
    for (String prefix : this.removeLeadingPath) {
      if (!prefix.endsWith("/")) {
        throw new IllegalArgumentException("Remove leading path must end with /, got: " + prefix);
      }
    }
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String uri = request.getRequestURI();
    for (String prefix : removeLeadingPath) {
      if (uri.startsWith(prefix)) {
        String newUri = uri.substring(prefix.length() - 1);
        request.getRequestDispatcher(newUri).forward(request, response);
        return;
      }
    }

    filterChain.doFilter(request, response);
  }

  /**
   * Provide the paths that are be removed as url pattern suitable for filter configuration. See
   * class usage.
   */
  public String[] removeLeadingPathsAsUrlPatterns() {
    return removeLeadingPath.stream()
        .map(p -> p + "*")
        .collect(Collectors.toList())
        .toArray(new String[removeLeadingPath.size()]);
  }
}
