package org.sihsalus.webservices.rest;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.ServiceContext;
import org.openmrs.api.context.UserContext;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.webservices.rest.web.OpenmrsClassScanner;
import org.openmrs.module.webservices.rest.web.api.RestHelperService;
import org.openmrs.module.webservices.rest.web.api.RestService;
import org.openmrs.module.webservices.rest.web.api.impl.RestHelperServiceImpl;
import org.openmrs.module.webservices.rest.web.api.impl.RestServiceImpl;
import org.openmrs.module.webservices.rest.web.filter.AuthorizationFilter;
import org.openmrs.module.webservices.rest.web.filter.ContentTypeFilter;
import org.openmrs.module.webservices.rest.web.v1_0.controller.MainResourceController;
import org.sihsalus.core.api.authorization.PatientObjectAuthorizationService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.Ordered;

@Configuration
@ComponentScan(
    basePackageClasses = MainResourceController.class,
    excludeFilters =
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern =
                "org\\.openmrs\\.module\\.webservices\\.rest\\.web\\.v1_0\\.controller\\.openmrs.*"))
public class SystemRestConfiguration {

  private static final String OPENMRS_USER_CONTEXT_SESSION_ATTRIBUTE =
      "org.sihsalus.openmrs.USER_CONTEXT";

  private static final String LEGACY_OPENMRS_PATH_PREFIX = "/openmrs";

  @Bean
  SystemStatusController systemStatusController() {
    return new SystemStatusController();
  }

  @Bean
  Filter legacyWsRestPathAliasFilter() {
    return new LegacyWsRestPathAliasFilter();
  }

  @Bean
  Filter openmrsRestContextSessionFilter() {
    return new OpenmrsRestContextSessionFilter();
  }

  @Bean
  Filter restContentTypeFilter() {
    return new PathScopedFilter(new ContentTypeFilter(), 1, "/rest/", "/ws/rest/");
  }

  @Bean
  Filter restAuthorizationFilter() {
    return new PathScopedFilter(
        new AuthorizationFilter(),
        2,
        "/rest/",
        "/ws/rest/",
        "/api/fhir/",
        "/ws/fhir2/",
        "/admin",
        "/admin/",
        "/api/admin/",
        "/api/system/",
        "/module/");
  }

  @Bean
  Filter patientObjectAuthorizationFilter() {
    return new PathScopedFilter(
        new PatientObjectAuthorizationFilter(), 3, "/rest/v1/patient/", "/ws/rest/v1/patient/");
  }

  @Bean
  OpenmrsClassScanner openmrsClassScanner() {
    return OpenmrsClassScanner.getInstance();
  }

  @Bean(destroyMethod = "shutdown")
  ExecutorService webservicesRestExecutor() {
    return Executors.newFixedThreadPool(5);
  }

  @Bean
  RestHelperService restHelperService(DbSessionFactory dbSessionFactory) {
    RestHelperServiceImpl service = new RestHelperServiceImpl();
    service.setSessionFactory(dbSessionFactory);
    return service;
  }

  @Bean
  RestService restService(
      RestHelperService restHelperService,
      OpenmrsClassScanner openmrsClassScanner,
      @Qualifier("webservicesRestExecutor") ExecutorService webservicesRestExecutor) {
    RestServiceImpl service = new RestServiceImpl();
    service.setRestHelperService(restHelperService);
    service.setOpenmrsClassScanner(openmrsClassScanner);
    service.setExecutorService(webservicesRestExecutor);
    return service;
  }

  @Bean
  StaticRestServiceRegistration staticRestServiceRegistration(
      ServiceContext serviceContext, RestService restService, RestHelperService restHelperService) {
    return new StaticRestServiceRegistration(serviceContext, restService, restHelperService);
  }

  static final class StaticRestServiceRegistration implements SmartLifecycle {

    private final ServiceContext serviceContext;

    private final RestService restService;

    private final RestHelperService restHelperService;

    private boolean running;

    StaticRestServiceRegistration(
        ServiceContext serviceContext,
        RestService restService,
        RestHelperService restHelperService) {
      this.serviceContext = serviceContext;
      this.restService = restService;
      this.restHelperService = restHelperService;
    }

    @Override
    public void start() {
      serviceContext.setService(RestService.class, restService);
      serviceContext.setService(RestHelperService.class, restHelperService);
      running = true;
    }

    @Override
    public void stop() {
      running = false;
    }

    @Override
    public boolean isRunning() {
      return running;
    }
  }

  static final class OpenmrsRestContextSessionFilter implements Filter, Ordered {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
      boolean openedSession = false;
      if (isOpenmrsWebRequest(request) && !Context.isSessionOpen()) {
        Context.openSession();
        openedSession = true;
        restoreUserContextFromHttpSession(request);
      }
      try {
        chain.doFilter(request, response);
      } finally {
        if (openedSession) {
          persistUserContextToHttpSession(request);
          Context.closeSession();
        }
      }
    }

    @Override
    public int getOrder() {
      return 0;
    }

    private boolean isOpenmrsWebRequest(ServletRequest request) {
      return request instanceof HttpServletRequest httpRequest
          && (httpRequest.getRequestURI().startsWith(httpRequest.getContextPath() + "/rest/")
              || httpRequest.getRequestURI().startsWith(httpRequest.getContextPath() + "/ws/rest/")
              || httpRequest.getRequestURI().startsWith(httpRequest.getContextPath() + "/api/fhir/")
              || httpRequest
                  .getRequestURI()
                  .startsWith(httpRequest.getContextPath() + "/ws/fhir2/"));
    }
  }

  static final class LegacyWsRestPathAliasFilter implements Filter, Ordered {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
      if (request instanceof HttpServletRequest httpRequest && isLegacyOpenmrsPath(httpRequest)) {
        chain.doFilter(new LegacyWsRestRequestWrapper(httpRequest), response);
        return;
      }

      chain.doFilter(request, response);
    }

    @Override
    public int getOrder() {
      return -1;
    }

    private boolean isLegacyWsRestPath(HttpServletRequest request) {
      String requestUri = request.getRequestURI();
      String legacyPrefix = request.getContextPath() + "/ws/rest";
      return requestUri.equals(legacyPrefix) || requestUri.startsWith(legacyPrefix + "/");
    }

    private boolean isLegacyOpenmrsPath(HttpServletRequest request) {
      return isLegacyWsRestPath(request) || usesOpenmrsPathPrefix(request);
    }
  }

  static final class PatientObjectAuthorizationFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
      if (request instanceof HttpServletRequest httpRequest
          && response instanceof HttpServletResponse httpResponse) {
        String patientUuid = patientUuidFromPath(httpRequest);
        if (patientUuid != null
            && !PatientObjectAuthorizationService.current().canReadPatient(patientUuid)) {
          httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Patient access denied");
          return;
        }
      }

      chain.doFilter(request, response);
    }

    private String patientUuidFromPath(HttpServletRequest request) {
      String path = request.getRequestURI().substring(request.getContextPath().length());
      String prefix = path.startsWith("/ws/rest/") ? "/ws/rest/v1/patient/" : "/rest/v1/patient/";
      if (!path.startsWith(prefix)) {
        return null;
      }
      String patientUuid = path.substring(prefix.length());
      int nextSegment = patientUuid.indexOf('/');
      if (nextSegment >= 0) {
        patientUuid = patientUuid.substring(0, nextSegment);
      }
      return patientUuid.isBlank() ? null : patientUuid;
    }
  }

  static final class LegacyWsRestRequestWrapper extends HttpServletRequestWrapper {

    private final String contextPath;

    private final String requestUri;

    private final String servletPath;

    private final String pathInfo;

    LegacyWsRestRequestWrapper(HttpServletRequest request) {
      super(request);
      this.contextPath = effectiveContextPath(request);
      this.requestUri =
          contextPath + normalizeOpenmrsPath(request.getRequestURI(), request.getContextPath());
      this.servletPath = normalizeOpenmrsPath(request.getServletPath(), "");
      this.pathInfo = normalizeOpenmrsPath(request.getPathInfo(), "");
    }

    @Override
    public String getContextPath() {
      return contextPath;
    }

    @Override
    public String getRequestURI() {
      return requestUri;
    }

    @Override
    public StringBuffer getRequestURL() {
      HttpServletRequest request = (HttpServletRequest) getRequest();
      String originalUri = request.getRequestURI();
      StringBuffer originalUrl = request.getRequestURL();
      int uriIndex = originalUrl.indexOf(originalUri);
      if (uriIndex < 0) {
        return new StringBuffer(originalUrl.toString().replace(originalUri, requestUri));
      }

      return new StringBuffer(originalUrl.substring(0, uriIndex)).append(requestUri);
    }

    @Override
    public String getServletPath() {
      return servletPath;
    }

    @Override
    public String getPathInfo() {
      return pathInfo;
    }

    private static String normalizeOpenmrsPath(String path, String contextPath) {
      if (path == null) {
        return null;
      }

      String pathWithoutContext =
          contextPath.isEmpty() ? path : path.substring(contextPath.length());
      if (pathWithoutContext.equals(LEGACY_OPENMRS_PATH_PREFIX)) {
        pathWithoutContext = "/";
      } else if (pathWithoutContext.startsWith(LEGACY_OPENMRS_PATH_PREFIX + "/")) {
        pathWithoutContext = pathWithoutContext.substring(LEGACY_OPENMRS_PATH_PREFIX.length());
      }
      if (pathWithoutContext.equals("/ws/rest")) {
        return "/rest";
      }
      if (pathWithoutContext.startsWith("/ws/rest/")) {
        return stripRestTrailingSlash(pathWithoutContext.substring("/ws".length()));
      }

      return stripRestTrailingSlash(pathWithoutContext);
    }

    /**
     * Spring 6 / Boot 3 dropped trailing-slash path matching, but OpenMRS REST clients (notably the
     * reference SPA's patient create, {@code POST /ws/rest/v1/patient/}) still send a trailing
     * slash. Trim a single trailing slash on {@code /rest} paths so they keep matching the
     * {@code {resource}} request mappings instead of 404ing.
     */
    private static String stripRestTrailingSlash(String path) {
      if (path.startsWith("/rest") && path.endsWith("/") && path.length() > "/rest".length()) {
        return path.substring(0, path.length() - 1);
      }
      return path;
    }

    private static String effectiveContextPath(HttpServletRequest request) {
      return usesOpenmrsPathPrefix(request)
          ? request.getContextPath() + LEGACY_OPENMRS_PATH_PREFIX
          : request.getContextPath();
    }
  }

  static final class PathScopedFilter implements Filter, Ordered {

    private final Filter delegate;

    private final int order;

    private final String[] pathPrefixes;

    PathScopedFilter(Filter delegate, int order, String... pathPrefixes) {
      this.delegate = delegate;
      this.order = order;
      this.pathPrefixes = pathPrefixes;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
      if (!matchesPath(request)) {
        chain.doFilter(request, response);
        return;
      }

      boolean openedSession = false;
      if (!Context.isSessionOpen()) {
        Context.openSession();
        openedSession = true;
        restoreUserContextFromHttpSession(request);
      }
      try {
        delegate.doFilter(request, response, chain);
      } finally {
        if (openedSession) {
          persistUserContextToHttpSession(request);
          Context.closeSession();
        }
      }
    }

    @Override
    public int getOrder() {
      return order;
    }

    private boolean matchesPath(ServletRequest request) {
      if (!(request instanceof HttpServletRequest httpRequest)) {
        return false;
      }

      String contextPath = httpRequest.getContextPath();
      String requestUri = httpRequest.getRequestURI();
      for (String pathPrefix : pathPrefixes) {
        String scopedPath = contextPath + pathPrefix;
        if (pathPrefix.endsWith("/") && requestUri.startsWith(scopedPath)) {
          return true;
        }
        if (!pathPrefix.endsWith("/") && requestUri.equals(scopedPath)) {
          return true;
        }
      }

      return false;
    }
  }

  private static void restoreUserContextFromHttpSession(ServletRequest request) {
    if (!(request instanceof HttpServletRequest httpRequest)) {
      return;
    }

    HttpSession httpSession = httpRequest.getSession(false);
    if (httpSession == null) {
      return;
    }

    Object storedContext = httpSession.getAttribute(OPENMRS_USER_CONTEXT_SESSION_ATTRIBUTE);
    if (storedContext instanceof UserContext userContext) {
      Context.setUserContext(userContext);
    }
  }

  private static void persistUserContextToHttpSession(ServletRequest request) {
    if (!(request instanceof HttpServletRequest httpRequest)) {
      return;
    }

    if (Context.isAuthenticated()) {
      httpRequest
          .getSession(true)
          .setAttribute(OPENMRS_USER_CONTEXT_SESSION_ATTRIBUTE, Context.getUserContext());
      return;
    }

    HttpSession httpSession = httpRequest.getSession(false);
    if (httpSession != null) {
      httpSession.removeAttribute(OPENMRS_USER_CONTEXT_SESSION_ATTRIBUTE);
    }
  }

  private static boolean usesOpenmrsPathPrefix(HttpServletRequest request) {
    String contextPath = request.getContextPath();
    if (!contextPath.isEmpty()) {
      return false;
    }

    String requestUri = request.getRequestURI();
    return requestUri.equals(LEGACY_OPENMRS_PATH_PREFIX)
        || requestUri.startsWith(LEGACY_OPENMRS_PATH_PREFIX + "/");
  }
}
