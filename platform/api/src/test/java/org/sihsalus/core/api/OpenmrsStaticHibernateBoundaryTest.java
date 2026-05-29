package org.sihsalus.core.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openmrs.api.db.hibernate.HibernateSessionFactoryBean;

class OpenmrsStaticHibernateBoundaryTest {

  private static final String OPENMRS_APPLICATION_DATA_DIRECTORY =
      "OPENMRS_APPLICATION_DATA_DIRECTORY";

  @TempDir Path openmrsApplicationDirectory;

  @Test
  void hibernateMappingsDoNotUseDynamicModuleDiscovery() throws Exception {
    System.setProperty(OPENMRS_APPLICATION_DATA_DIRECTORY, openmrsApplicationDirectory.toString());
    Files.createDirectories(openmrsApplicationDirectory.resolve("configuration"));

    HibernateSessionFactoryBean sessionFactory = new HibernateSessionFactoryBean();

    assertTrue(sessionFactory.getModulePackagesWithMappedClasses().isEmpty());
    assertTrue(sessionFactory.getModuleMappingResources().isEmpty());

    String source =
        Files.readString(
            Path.of("src/main/java/org/openmrs/api/db/hibernate/HibernateSessionFactoryBean.java"));
    assertFalse(source.contains("ModuleFactory"));
    assertFalse(source.contains("getStartedModules"));
  }
}
