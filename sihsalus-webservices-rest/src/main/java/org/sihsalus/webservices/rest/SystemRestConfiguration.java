package org.sihsalus.webservices.rest;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.ServiceContext;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.webservices.rest.web.OpenmrsClassScanner;
import org.openmrs.module.webservices.rest.web.api.RestHelperService;
import org.openmrs.module.webservices.rest.web.api.RestService;
import org.openmrs.module.webservices.rest.web.api.impl.RestHelperServiceImpl;
import org.openmrs.module.webservices.rest.web.api.impl.RestServiceImpl;
import org.openmrs.module.webservices.rest.web.v1_0.controller.MainResourceController;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.SmartLifecycle;

@Configuration
@ComponentScan(basePackageClasses = MainResourceController.class)
public class SystemRestConfiguration {

    @Bean
    SystemStatusController systemStatusController() {
        return new SystemStatusController();
    }

    @Bean
    Filter openmrsRestContextSessionFilter() {
        return new OpenmrsRestContextSessionFilter();
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
                ServiceContext serviceContext, RestService restService, RestHelperService restHelperService) {
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

    static final class OpenmrsRestContextSessionFilter implements Filter {

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            boolean openedSession = false;
            if (isRestRequest(request) && !Context.isSessionOpen()) {
                Context.openSession();
                openedSession = true;
            }
            try {
                chain.doFilter(request, response);
            } finally {
                if (openedSession) {
                    Context.closeSession();
                }
            }
        }

        private boolean isRestRequest(ServletRequest request) {
            return request instanceof HttpServletRequest httpRequest
                    && httpRequest.getRequestURI().startsWith(httpRequest.getContextPath() + "/rest/");
        }
    }
}
