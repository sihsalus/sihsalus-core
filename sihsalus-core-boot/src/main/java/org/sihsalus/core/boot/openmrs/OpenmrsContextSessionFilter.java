package org.sihsalus.core.boot.openmrs;

import java.io.IOException;
import org.openmrs.api.context.Context;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
final class OpenmrsContextSessionFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        boolean openedSession = !Context.isSessionOpen();
        if (openedSession) {
            Context.openSession();
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            if (openedSession) {
                Context.closeSession();
            }
        }
    }
}
