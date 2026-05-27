package org.sihsalus.core.boot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.openmrs.module.openconceptlab.OpenConceptLabConstants;
import org.sihsalus.initializer.InitializerBoundary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@EnabledIfSystemProperty(
    named = "sihsalus.static-content.import-test.enabled",
    matches = "true",
    disabledReason = "static content import test is opt-in")
class SihsalusStaticContentImportTest {

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void staticContentAndOclPackagesLoadIntoBootDatabase() {
    assumeTrue(
        sihsalusContentConfigurationAvailable(),
        ".dev/reference-sources/sihsalus-content is a local reference clone and is not present in this checkout");

    assertEquals(
        1,
        countRows(
            "select count(*) from location where uuid = ?",
            "35d2234e-129a-4c40-abb2-1ae0b72c1602"));
    assertEquals(
        1, countRows("select count(*) from role where role = ?", "Organizational: Doctor"));
    assertEquals(
        1, countRows("select count(*) from privilege where privilege = ?", "O3 Implementer Tools"));
    assertTrue(
        countRows("select count(*) from address_hierarchy_entry") > 90000,
        "Peru address hierarchy entries should be loaded from sihsalus-content");
    assertTrue(
        countRows(
                "select count(*) from concept where uuid = ?",
                "4bf3f465-ac91-44fa-9b1f-173daf0c89a0")
            > 0,
        "OCL concepts should be imported by the dedicated static content test");
    assertTrue(
        countRows(
                "select count(*) from global_property where property like ?",
                "sihsalus.ocl.staticImport.sha256.%")
            > 0,
        "OCL import should leave an idempotency marker");
    assertTrue(
        countRows(
                "select count(*) from global_property where property = ? and property_value is not null",
                OpenConceptLabConstants.GP_OCL_LOAD_AT_STARTUP_PATH)
            > 0,
        "OCL load-at-startup path should be configured");
  }

  private int countRows(String sql, Object... args) {
    Integer count = jdbcTemplate.queryForObject(sql, Integer.class, args);
    return count == null ? 0 : count;
  }

  private boolean sihsalusContentConfigurationAvailable() {
    Path sourceLayout = Paths.get(InitializerBoundary.sourceLayout());
    Path current = Paths.get("").toAbsolutePath().normalize();
    for (Path candidate = current; candidate != null; candidate = candidate.getParent()) {
      Path configRoot =
          candidate
              .resolve(sourceLayout)
              .resolve("configuration/backend_configuration")
              .normalize();
      if (Files.isDirectory(configRoot)) {
        return true;
      }
    }
    return false;
  }
}
