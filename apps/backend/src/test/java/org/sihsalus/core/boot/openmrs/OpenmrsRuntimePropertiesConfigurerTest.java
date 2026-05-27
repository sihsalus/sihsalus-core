package org.sihsalus.core.boot.openmrs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.openmrs.module.initializer.InitializerConstants.PROPS_DOMAINS;
import static org.openmrs.module.initializer.InitializerConstants.PROPS_EXCLUDE;
import static org.openmrs.module.initializer.InitializerConstants.PROPS_STARTUP_LOAD;

import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.api.context.Context;
import org.openmrs.util.OpenmrsConstants;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.mock.env.MockEnvironment;

class OpenmrsRuntimePropertiesConfigurerTest {

  private Properties originalRuntimeProperties;

  private String originalApplicationDataDirectory;

  @BeforeEach
  void setUp() {
    originalRuntimeProperties = Context.getRuntimeProperties();
    originalApplicationDataDirectory =
        System.getProperty(OpenmrsConstants.KEY_OPENMRS_APPLICATION_DATA_DIRECTORY);
  }

  @AfterEach
  void tearDown() {
    Context.setRuntimeProperties(originalRuntimeProperties);
    if (originalApplicationDataDirectory == null) {
      System.clearProperty(OpenmrsConstants.KEY_OPENMRS_APPLICATION_DATA_DIRECTORY);
    } else {
      System.setProperty(
          OpenmrsConstants.KEY_OPENMRS_APPLICATION_DATA_DIRECTORY,
          originalApplicationDataDirectory);
    }
  }

  @Test
  void mapsLegacyOpenmrsApplicationDataDirectoryEnvironmentVariable() {
    runConfigurer(
        new MockEnvironment()
            .withProperty("spring.datasource.url", "jdbc:postgresql://localhost:5432/sihsalus")
            .withProperty("spring.datasource.username", "sihsalus")
            .withProperty("spring.datasource.password", "secret")
            .withProperty(
                OpenmrsConstants.KEY_OPENMRS_APPLICATION_DATA_DIRECTORY, "/openmrs/data"));

    assertThat(
            Context.getRuntimeProperties()
                .getProperty(OpenmrsConstants.APPLICATION_DATA_DIRECTORY_RUNTIME_PROPERTY))
        .isEqualTo("/openmrs/data");
    assertThat(System.getProperty(OpenmrsConstants.KEY_OPENMRS_APPLICATION_DATA_DIRECTORY))
        .isEqualTo("/openmrs/data");
  }

  @Test
  void prefersSihsalusApplicationDataDirectoryProperty() {
    runConfigurer(
        new MockEnvironment()
            .withProperty("spring.datasource.url", "jdbc:h2:mem:sihsalus")
            .withProperty("sihsalus.openmrs.application-data-directory", "/preferred")
            .withProperty(OpenmrsConstants.KEY_OPENMRS_APPLICATION_DATA_DIRECTORY, "/legacy"));

    assertThat(
            Context.getRuntimeProperties()
                .getProperty(OpenmrsConstants.APPLICATION_DATA_DIRECTORY_RUNTIME_PROPERTY))
        .isEqualTo("/preferred");
    assertThat(System.getProperty(OpenmrsConstants.KEY_OPENMRS_APPLICATION_DATA_DIRECTORY))
        .isEqualTo("/preferred");
  }

  @Test
  void mapsInitializerSettingsToOpenmrsRuntimeProperties() {
    runConfigurer(
        new MockEnvironment()
            .withProperty("spring.datasource.url", "jdbc:h2:mem:sihsalus")
            .withProperty("SIHSALUS_INITIALIZER_STARTUP_LOAD", "disabled")
            .withProperty("SIHSALUS_INITIALIZER_DOMAINS", "!addresshierarchy")
            .withProperty("SIHSALUS_INITIALIZER_EXCLUDE_ADDRESSHIERARCHY", "**/*.xml"));

    Properties runtimeProperties = Context.getRuntimeProperties();

    assertThat(runtimeProperties.getProperty(PROPS_STARTUP_LOAD)).isEqualTo("disabled");
    assertThat(runtimeProperties.getProperty(PROPS_DOMAINS)).isEqualTo("!addresshierarchy");
    assertThat(runtimeProperties.getProperty(PROPS_EXCLUDE + ".addresshierarchy"))
        .isEqualTo("**/*.xml");
  }

  @Test
  void rejectsPostgresqlRuntimeWithoutDatasourceUsername() {
    MockEnvironment environment =
        new MockEnvironment()
            .withProperty("spring.datasource.url", "jdbc:postgresql://localhost:5432/sihsalus")
            .withProperty("spring.datasource.password", "secret");

    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> runConfigurer(environment));

    assertThat(exception).hasMessageContaining("SIHSALUS_DATASOURCE_USERNAME");
  }

  @Test
  void rejectsPostgresqlRuntimeWithoutDatasourcePassword() {
    MockEnvironment environment =
        new MockEnvironment()
            .withProperty("spring.datasource.url", "jdbc:postgresql://localhost:5432/sihsalus")
            .withProperty("spring.datasource.username", "sihsalus");

    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> runConfigurer(environment));

    assertThat(exception).hasMessageContaining("SIHSALUS_DATASOURCE_PASSWORD");
  }

  private void runConfigurer(MockEnvironment environment) {
    OpenmrsRuntimePropertiesConfigurer configurer = new OpenmrsRuntimePropertiesConfigurer();
    configurer.setEnvironment(environment);
    configurer.postProcessBeanFactory(new DefaultListableBeanFactory());
  }
}
