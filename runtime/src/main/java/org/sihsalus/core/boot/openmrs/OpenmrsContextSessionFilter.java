package org.sihsalus.core.boot.openmrs;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.UserContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
final class OpenmrsContextSessionFilter extends OncePerRequestFilter {

  private static final String OPENMRS_USER_CONTEXT_SESSION_ATTRIBUTE =
      "org.sihsalus.openmrs.USER_CONTEXT";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    boolean openedSession = !Context.isSessionOpen();
    if (openedSession) {
      Context.openSession();
      restoreUserContextFromHttpSession(request);
    }
    try {
      filterChain.doFilter(request, response);
    } finally {
      if (openedSession) {
        persistUserContextToHttpSession(request);
        Context.closeSession();
      }
    }
  }

  private void restoreUserContextFromHttpSession(HttpServletRequest request) {
    HttpSession httpSession = request.getSession(false);
    if (httpSession == null) {
      return;
    }

    Object storedContext = httpSession.getAttribute(OPENMRS_USER_CONTEXT_SESSION_ATTRIBUTE);
    if (storedContext instanceof UserContext userContext) {
      Context.setUserContext(userContext);
    }
  }

  private void persistUserContextToHttpSession(HttpServletRequest request) {
    if (Context.isAuthenticated()) {
      request
          .getSession(true)
          .setAttribute(OPENMRS_USER_CONTEXT_SESSION_ATTRIBUTE, Context.getUserContext());
      return;
    }

    HttpSession httpSession = request.getSession(false);
    if (httpSession != null) {
      httpSession.removeAttribute(OPENMRS_USER_CONTEXT_SESSION_ATTRIBUTE);
    }
  }
}
