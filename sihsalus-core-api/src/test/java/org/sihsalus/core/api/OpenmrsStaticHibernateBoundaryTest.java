package org.sihsalus.core.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.openmrs.api.db.hibernate.HibernateSessionFactoryBean;

class OpenmrsStaticHibernateBoundaryTest {

    @Test
    void hibernateMappingsDoNotUseDynamicModuleDiscovery() throws Exception {
        HibernateSessionFactoryBean sessionFactory = new HibernateSessionFactoryBean();

        assertTrue(sessionFactory.getModulePackagesWithMappedClasses().isEmpty());
        assertTrue(sessionFactory.getModuleMappingResources().isEmpty());

        String source =
                Files.readString(
                        Path.of(
                                "src/main/java/org/openmrs/api/db/hibernate/HibernateSessionFactoryBean.java"));
        assertFalse(source.contains("ModuleFactory"));
        assertFalse(source.contains("getStartedModules"));
    }
}
