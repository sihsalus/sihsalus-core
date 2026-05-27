package org.sihsalus.module.openconceptlab;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SihsalusOpenConceptLabStaticContentImporterTest {

  @TempDir private Path tempDir;

  private String previousSourceRoot;

  @BeforeEach
  void setUp() {
    previousSourceRoot = System.getProperty("sihsalus.initializer.sourceRoot");
    System.setProperty("sihsalus.initializer.sourceRoot", tempDir.toString());
  }

  @AfterEach
  void restoreSourceRoot() {
    if (previousSourceRoot == null) {
      System.clearProperty("sihsalus.initializer.sourceRoot");
    } else {
      System.setProperty("sihsalus.initializer.sourceRoot", previousSourceRoot);
    }
  }

  @Test
  void enabledImporterFailsWhenStaticContentRootIsMissing() {
    SihsalusOpenConceptLabStaticContentImporter importer =
        new SihsalusOpenConceptLabStaticContentImporter(null, null, null, null, true, true);

    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> importer.onApplicationEvent(null));

    assertTrue(
        exception.getMessage().contains("SIH Salus content configuration root is not available"));
  }

  @Test
  void disabledImporterDoesNotRequireStaticContentRoot() {
    SihsalusOpenConceptLabStaticContentImporter importer =
        new SihsalusOpenConceptLabStaticContentImporter(null, null, null, null, false, true);

    assertDoesNotThrow(() -> importer.onApplicationEvent(null));
  }
}
