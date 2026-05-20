package org.sihsalus.core.boot;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
final class RequestTraceFilter extends OncePerRequestFilter implements Ordered {

    private static final Logger log = LoggerFactory.getLogger(RequestTraceFilter.class);

    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    private static final String MDC_REQUEST_ID = "requestId";

    private static final Pattern SAFE_REQUEST_ID = Pattern.compile("[A-Za-z0-9._-]{1,128}");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = requestId(request.getHeader(REQUEST_ID_HEADER));
        long startedAt = System.nanoTime();
        response.setHeader(REQUEST_ID_HEADER, requestId);
        MDC.put(MDC_REQUEST_ID, requestId);
        try {
            filterChain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException ex) {
            log.warn(
                    "Unhandled request exception method={} uri={} durationMs={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    elapsedMillis(startedAt),
                    ex);
            throw ex;
        } finally {
            int status = response.getStatus();
            if (status >= 500) {
                log.warn(
                        "HTTP request completed with server error method={} uri={} status={} durationMs={}",
                        request.getMethod(),
                        request.getRequestURI(),
                        status,
                        elapsedMillis(startedAt));
            }
            MDC.remove(MDC_REQUEST_ID);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private String requestId(String candidate) {
        if (candidate != null && SAFE_REQUEST_ID.matcher(candidate).matches()) {
            return candidate;
        }
        return UUID.randomUUID().toString();
    }

    private long elapsedMillis(long startedAt) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }
}
