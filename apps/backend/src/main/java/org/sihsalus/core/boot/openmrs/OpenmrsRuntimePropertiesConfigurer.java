package org.sihsalus.core.boot.openmrs;

import static org.openmrs.module.initializer.InitializerConstants.PROPS_DOMAINS;
import static org.openmrs.module.initializer.InitializerConstants.PROPS_EXCLUDE;
import static org.openmrs.module.initializer.InitializerConstants.PROPS_STARTUP_LOAD;

import java.util.Locale;
import java.util.Properties;
import org.openmrs.api.context.Context;
import org.openmrs.module.authentication.AuthenticationConfig;
import org.openmrs.module.oauth2login.OAuth2LoginConstants;
import org.openmrs.util.OpenmrsConstants;
import org.openmrs.util.OpenmrsUtil;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

final class OpenmrsRuntimePropertiesConfigurer
    implements BeanFactoryPostProcessor, EnvironmentAware {

  private static final String AUTH_MODE_FRONTEND = "frontend";

  private static final String AUTH_MODE_KEYCLOAK = "keycloak";

  private Environment environment;

  @Override
  public void setEnvironment(Environment environment) {
    this.environment = environment;
  }

  @Override
  public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
      throws BeansException {
    Properties properties = new Properties();

    String datasourceUrl = required("spring.datasource.url");
    String driverClass = property("spring.datasource.driver-class-name", driverFor(datasourceUrl));
    String datasourceUsername = property("spring.datasource.username", "");
    String datasourcePassword = property("spring.datasource.password", "");
    validateDatasourceCredentials(datasourceUrl, datasourceUsername, datasourcePassword);

    properties.setProperty("connection.url", datasourceUrl);
    properties.setProperty("connection.username", datasourceUsername);
    properties.setProperty("connection.password", datasourcePassword);
    properties.setProperty("connection.driver_class", driverClass);
    properties.setProperty("hibernate.connection.url", datasourceUrl);
    properties.setProperty("hibernate.connection.username", datasourceUsername);
    properties.setProperty("hibernate.connection.password", datasourcePassword);
    properties.setProperty("hibernate.connection.driver_class", driverClass);
    properties.setProperty("hibernate.dialect", dialectFor(datasourceUrl));
    properties.setProperty("hibernate.hbm2ddl.auto", "none");
    properties.setProperty("hibernate.cache.use_second_level_cache", "false");
    properties.setProperty("hibernate.cache.use_query_cache", "false");
    properties.setProperty("cache.type", "local");

    String applicationDataDirectory =
        firstProperty(
            "sihsalus.openmrs.application-data-directory",
            "openmrs.application-data-directory",
            "openmrs.application.data.directory",
            OpenmrsConstants.KEY_OPENMRS_APPLICATION_DATA_DIRECTORY);
    if (applicationDataDirectory == null) {
      applicationDataDirectory = defaultApplicationDataDirectory();
    }
    properties.setProperty(
        OpenmrsConstants.APPLICATION_DATA_DIRECTORY_RUNTIME_PROPERTY, applicationDataDirectory);
    OpenmrsUtil.setApplicationDataDirectory(applicationDataDirectory);

    copyOptionalOpenmrsRuntimeProperty(
        properties,
        PROPS_STARTUP_LOAD,
        "sihsalus.initializer.startup-load",
        "SIHSALUS_INITIALIZER_STARTUP_LOAD",
        PROPS_STARTUP_LOAD,
        "INITIALIZER_STARTUP_LOAD");
    copyOptionalOpenmrsRuntimeProperty(
        properties,
        PROPS_DOMAINS,
        "sihsalus.initializer.domains",
        "SIHSALUS_INITIALIZER_DOMAINS",
        PROPS_DOMAINS,
        "INITIALIZER_DOMAINS");
    copyOptionalOpenmrsRuntimeProperty(
        properties,
        PROPS_EXCLUDE + ".addresshierarchy",
        "sihsalus.initializer.exclude.addresshierarchy",
        "SIHSALUS_INITIALIZER_EXCLUDE_ADDRESSHIERARCHY",
        PROPS_EXCLUDE + ".addresshierarchy",
        "INITIALIZER_EXCLUDE_ADDRESSHIERARCHY");

    configureAuthentication(properties);

    Context.setRuntimeProperties(properties);
    AuthenticationConfig.setConfig(null);
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

  private String firstProperty(String... keys) {
    for (String key : keys) {
      String value = environment.getProperty(key);
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return null;
  }

  private void copyOptionalOpenmrsRuntimeProperty(
      Properties properties, String targetKey, String... sourceKeys) {
    String value = firstProperty(sourceKeys);
    if (value != null) {
      properties.setProperty(targetKey, value);
    }
  }

  private void configureAuthentication(Properties properties) {
    String authMode = authenticationMode();
    boolean keycloakMode = AUTH_MODE_KEYCLOAK.equals(authMode);

    properties.setProperty(
        OAuth2LoginConstants.OAUTH2_ENABLED_PROPERTY, Boolean.toString(keycloakMode));
    if (keycloakMode) {
      properties.setProperty(AuthenticationConfig.SCHEME, OAuth2LoginConstants.OAUTH2_SCHEME_ID);
    }
  }

  private String authenticationMode() {
    String configuredMode = firstProperty("sihsalus.auth.mode", "SIHSALUS_AUTH_MODE");
    if (configuredMode != null) {
      return normalizeAuthenticationMode(configuredMode);
    }

    String oauth2Enabled =
        firstProperty(
            "sihsalus.auth.oauth2-enabled",
            "OAUTH2_ENABLED",
            OAuth2LoginConstants.OAUTH2_ENABLED_PROPERTY);
    if (Boolean.parseBoolean(oauth2Enabled)) {
      return AUTH_MODE_KEYCLOAK;
    }
    return AUTH_MODE_FRONTEND;
  }

  private static String normalizeAuthenticationMode(String authMode) {
    String normalized = authMode.trim().toLowerCase(Locale.ROOT);
    switch (normalized) {
      case "frontend":
      case "local":
      case "openmrs":
      case "basic":
        return AUTH_MODE_FRONTEND;
      case "keycloak":
      case "oauth2":
        return AUTH_MODE_KEYCLOAK;
      default:
        throw new IllegalStateException(
            "Unsupported SIHSALUS_AUTH_MODE: " + authMode + " (expected frontend or keycloak)");
    }
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

  private static void validateDatasourceCredentials(
      String datasourceUrl, String datasourceUsername, String datasourcePassword) {
    if (!datasourceUrl.startsWith("jdbc:postgresql:")) {
      return;
    }
    if (datasourceUsername == null || datasourceUsername.isBlank()) {
      throw new IllegalStateException(
          "SIHSALUS_DATASOURCE_USERNAME must be set for PostgreSQL runtime");
    }
    if (datasourcePassword == null || datasourcePassword.isBlank()) {
      throw new IllegalStateException(
          "SIHSALUS_DATASOURCE_PASSWORD must be set for PostgreSQL runtime");
    }
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
