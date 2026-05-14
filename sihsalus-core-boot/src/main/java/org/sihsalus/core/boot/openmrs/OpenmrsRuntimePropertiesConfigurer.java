package org.sihsalus.core.boot.openmrs;

import java.util.Properties;
import org.openmrs.api.context.Context;
import org.openmrs.util.OpenmrsConstants;
import org.openmrs.util.OpenmrsUtil;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

final class OpenmrsRuntimePropertiesConfigurer implements BeanFactoryPostProcessor, EnvironmentAware {

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        Properties properties = new Properties();

        String datasourceUrl = required("spring.datasource.url");
        String driverClass = property("spring.datasource.driver-class-name", driverFor(datasourceUrl));

        properties.setProperty("connection.url", datasourceUrl);
        properties.setProperty("connection.username", property("spring.datasource.username", ""));
        properties.setProperty("connection.password", property("spring.datasource.password", ""));
        properties.setProperty("connection.driver_class", driverClass);
        properties.setProperty("hibernate.connection.url", datasourceUrl);
        properties.setProperty("hibernate.connection.username", property("spring.datasource.username", ""));
        properties.setProperty("hibernate.connection.password", property("spring.datasource.password", ""));
        properties.setProperty("hibernate.connection.driver_class", driverClass);
        properties.setProperty("hibernate.dialect", dialectFor(datasourceUrl));
        properties.setProperty("hibernate.hbm2ddl.auto", "none");
        properties.setProperty("hibernate.cache.use_second_level_cache", "false");
        properties.setProperty("hibernate.cache.use_query_cache", "false");
        properties.setProperty("cache.type", "local");

        String applicationDataDirectory =
                property("sihsalus.openmrs.application-data-directory", defaultApplicationDataDirectory());
        properties.setProperty(OpenmrsConstants.APPLICATION_DATA_DIRECTORY_RUNTIME_PROPERTY, applicationDataDirectory);
        OpenmrsUtil.setApplicationDataDirectory(applicationDataDirectory);

        Context.setRuntimeProperties(properties);
    }

    private String required(String key) {
        String value = environment.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required property: " + key);
        }
        return value;
    }

    private String property(String key, String defaultValue) {
        String value = environment.getProperty(key);
        return value == null ? defaultValue : value;
    }

    private static String driverFor(String datasourceUrl) {
        if (datasourceUrl.startsWith("jdbc:h2:")) {
            return "org.h2.Driver";
        }
        if (datasourceUrl.startsWith("jdbc:postgresql:")) {
            return "org.postgresql.Driver";
        }
        throw new IllegalArgumentException("Unsupported OpenMRS datasource URL: " + datasourceUrl);
    }

    private static String dialectFor(String datasourceUrl) {
        if (datasourceUrl.startsWith("jdbc:h2:")) {
            return "org.hibernate.dialect.H2Dialect";
        }
        if (datasourceUrl.startsWith("jdbc:postgresql:")) {
            return "org.hibernate.dialect.PostgreSQLDialect";
        }
        throw new IllegalArgumentException("Unsupported OpenMRS datasource URL: " + datasourceUrl);
    }

    private static String defaultApplicationDataDirectory() {
        return System.getProperty("java.io.tmpdir") + "/sihsalus-openmrs";
    }
}
