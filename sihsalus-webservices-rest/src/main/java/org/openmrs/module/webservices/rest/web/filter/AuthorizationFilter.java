/**
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * OpenMRS is also distributed under the terms of the Healthcare Disclaimer located at
 * http://openmrs.org/license.
 *
 * <p>Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS graphic logo is a
 * trademark of OpenMRS Inc.
 */
package org.openmrs.module.webservices.rest.web.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.api.context.Context;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.RestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filter intended for all /ws/rest calls that allows the user to authenticate via Basic
 * authentication. (It will not fail on invalid or missing credentials. We count on the API to throw
 * exceptions if an unauthenticated user tries to do something they are not allowed to do.) <br>
 * <br>
 * IP address authorization is also performed based on the global property: {@link
 * RestConstants#ALLOWED_IPS_GLOBAL_PROPERTY_NAME}
 */
public class AuthorizationFilter implements Filter {

  private static final Logger log = LoggerFactory.getLogger(AuthorizationFilter.class);

  /**
   * @see jakarta.servlet.Filter#init(jakarta.servlet.FilterConfig)
   */
  @Override
  public void init(FilterConfig arg0) throws ServletException {
    log.debug("Initializing REST WS Authorization filter");
  }

  /**
   * @see jakarta.servlet.Filter#destroy()
   */
  @Override
  public void destroy() {
    log.debug("Destroying REST WS Authorization filter");
  }

  /**
   * @see jakarta.servlet.Filter#doFilter(jakarta.servlet.ServletRequest,
   *     jakarta.servlet.ServletResponse, jakarta.servlet.FilterChain)
   */
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    // check the IP address first.  If its not valid, return a 403
    if (!RestUtil.isIpAllowed(request.getRemoteAddr())) {
      // the ip address is not valid, set a 403 http error code
      HttpServletResponse httpresponse = (HttpServletResponse) response;
      httpresponse.sendError(
          HttpServletResponse.SC_FORBIDDEN,
          "IP address '" + request.getRemoteAddr() + "' is not authorized");
      return;
    }

    if (request instanceof HttpServletRequest) {
      HttpServletRequest httpRequest = (HttpServletRequest) request;
      if (httpRequest.getRequestedSessionId() != null && !httpRequest.isRequestedSessionIdValid()) {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Session timed out");
        return;
      }

      if (!Context.isAuthenticated()) {
        String authorizationHeader = httpRequest.getHeader("Authorization");
        if (authorizationHeader != null) {
          // check that header is in format "Basic ${base64encode(username + ":" + password)}"
          if (StringUtils.startsWithIgnoreCase(authorizationHeader, "Basic ")) {
            try {
              // remove the leading "Basic "
              String basicAuth = authorizationHeader.substring(6).trim();
              if (StringUtils.isBlank(basicAuth)) {
                HttpServletResponse httpResponse = (HttpServletResponse) response;
                httpResponse.sendError(
                    HttpServletResponse.SC_BAD_REQUEST, "Invalid credentials provided");
                return;
              }

              String decoded =
                  new String(Base64.getDecoder().decode(basicAuth), StandardCharsets.UTF_8);
              int separator = decoded.indexOf(":");
              if (StringUtils.isBlank(decoded) || separator < 0) {
                HttpServletResponse httpResponse = (HttpServletResponse) response;
                httpResponse.sendError(
                    HttpServletResponse.SC_BAD_REQUEST, "Invalid credentials provided");
                return;
              }

              String username = decoded.substring(0, separator);
              String password = decoded.substring(separator + 1);
              Context.authenticate(username, password);
              log.debug("authenticated [{}]", username);
            } catch (Exception ex) {
              log.debug("authentication exception ", ex);
              sendUnauthorized(response, "Invalid username or password");
              return;
            }
          } else {
            sendUnauthorized(response, "Unsupported authorization scheme");
            return;
          }
        }
      }

      if (!Context.isAuthenticated() && requiresAuthentication(httpRequest)) {
        sendUnauthorized(response, "Authentication required");
        return;
      }
    }

    // continue with the filter chain (unless IP is not allowed)
    chain.doFilter(request, response);
  }

  private boolean requiresAuthentication(HttpServletRequest request) {
    return !"OPTIONS".equalsIgnoreCase(request.getMethod()) && !isSessionResource(request);
  }

  private boolean isSessionResource(HttpServletRequest request) {
    String contextPath = request.getContextPath();
    String requestUri = request.getRequestURI();
    return requestUri.equals(contextPath + "/rest/" + RestConstants.VERSION_1 + "/session")
        || requestUri.equals(contextPath + "/ws/rest/" + RestConstants.VERSION_1 + "/session");
  }

  private void sendUnauthorized(ServletResponse response, String message) throws IOException {
    HttpServletResponse httpResponse = (HttpServletResponse) response;
    httpResponse.setHeader("WWW-Authenticate", "Basic realm=\"OpenMRS REST\"");
    httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, message);
  }
}
