package org.sihsalus.module.event;

import org.openmrs.event.EventActivator;
import org.openmrs.event.JmsEventPublisher;
import org.openmrs.event.TransactionEventListener;
import org.openmrs.event.api.db.hibernate.HibernateEventInterceptor;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackageClasses = {HibernateEventInterceptor.class, JmsEventPublisher.class})
public class SihsalusEventConfiguration {

    @Bean
    EventActivator eventActivator() {
        return new EventActivator();
    }

    @Bean
    SmartInitializingSingleton eventActivatorStarter(EventActivator eventActivator) {
        return () -> {
            TransactionEventListener.setDaemonToken(null);
            eventActivator.started();
        };
    }

    @Bean
    DisposableBean eventActivatorStopper(EventActivator eventActivator) {
        return eventActivator::stopped;
    }
}
