package gov.va.api.health.autoconfig.rest;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PathRewriteFilterTest {

  @Mock HttpServletRequest request;

  @Mock HttpServletResponse response;

  @Mock FilterChain chain;
  @Mock RequestDispatcher dispatcher;

  @Test
  @SneakyThrows
  void matchingPathIsStripped() {
    when(request.getRequestURI()).thenReturn("/killme/ok");
    when(request.getRequestDispatcher(anyString())).thenReturn(dispatcher);

    var filter = PathRewriteFilter.builder().removeLeadingPath("/killme/").build();
    filter.doFilter(request, response, chain);
    verify(request).getRequestDispatcher("/ok");
    verifyNoInteractions(chain);
  }

  @Test
  void noSlashNoService() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            PathRewriteFilter.builder()
                .removeLeadingPath("/ok/")
                .removeLeadingPath("/nah")
                .build());
  }

  @Test
  @SneakyThrows
  void nonMatchingPathIsNotStripped() {
    when(request.getRequestURI()).thenReturn("/ok");

    var filter = PathRewriteFilter.builder().removeLeadingPath("/killme/").build();
    filter.doFilter(request, response, chain);
    verifyNoInteractions(dispatcher);
    verify(chain).doFilter(request, response);
  }
}
