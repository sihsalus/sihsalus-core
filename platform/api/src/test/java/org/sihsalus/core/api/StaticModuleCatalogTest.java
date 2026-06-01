package org.sihsalus.core.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Structural invariants for {@link StaticModuleCatalog}. These replace the previous per-module
 * version-string assertions (which only checked that hardcoded strings matched other hardcoded
 * strings and required mechanical churn on every version bump) with checks that actually protect
 * the catalog from drift and incomplete entries.
 */
class StaticModuleCatalogTest {

  @Test
  void catalogIsNotEmptyAndEveryModuleIsStaticInternal() {
    assertFalse(StaticModuleCatalog.modules().isEmpty(), "catalog must not be empty");
    assertEquals(
        StaticModuleCatalog.modules().size(),
        StaticModuleCatalog.staticInternalModules().size(),
        "all catalog modules are expected to be STATIC_INTERNAL");
    for (SihsalusModuleDescriptor module : StaticModuleCatalog.modules()) {
      assertEquals(
          SihsalusModuleStatus.STATIC_INTERNAL,
          module.status(),
          "unexpected status for module " + module.id());
    }
  }

  @Test
  void moduleIdsAreUnique() {
    Set<String> seen = new HashSet<>();
    for (SihsalusModuleDescriptor module : StaticModuleCatalog.modules()) {
      assertTrue(seen.add(module.id()), "duplicate module id: " + module.id());
    }
  }

  @Test
  void everyModuleHasCompleteRequiredMetadata() {
    for (SihsalusModuleDescriptor module : StaticModuleCatalog.modules()) {
      assertNotBlank(module.id(), "id");
      assertNotBlank(module.sourceModule(), "sourceModule for " + module.id());
      assertNotBlank(module.baselineVersion(), "baselineVersion for " + module.id());
      assertNotBlank(module.configBean(), "configBean for " + module.id());
      assertNotNull(module.status(), "status for " + module.id());

      // Optional metadata, when present, must not be blank and must agree with the helper flags.
      if (module.liquibaseFile() != null) {
        assertFalse(module.liquibaseFile().isBlank(), "blank liquibaseFile for " + module.id());
        assertTrue(module.isDatabaseManaged(), "isDatabaseManaged mismatch for " + module.id());
      }
      if (module.schedulerPrefix() != null) {
        assertFalse(module.schedulerPrefix().isBlank(), "blank schedulerPrefix for " + module.id());
        assertTrue(module.hasLegacyScheduler(), "hasLegacyScheduler mismatch for " + module.id());
      }
    }
  }

  @Test
  void includesFhirAndExcludesRetiredRuntimeModules() {
    Set<String> ids =
        StaticModuleCatalog.modules().stream()
            .map(SihsalusModuleDescriptor::id)
            .collect(Collectors.toSet());
    assertTrue(ids.contains("fhir2"));
    assertFalse(ids.contains("omod-runtime"));
    assertFalse(ids.contains("identitylookup"));
  }

  /**
   * Cross-checks the catalog against the Maven reactor: the set of {@code modules/<id>} entries in
   * the root pom must exactly equal the catalog ids. This catches the "added a module to the
   * reactor but forgot the catalog (or vice versa)" drift that previously had no build-time guard.
   */
  @Test
  void catalogMatchesMavenReactorModules() throws IOException {
    Path rootPom = locateRootPom();
    assumeTrue(rootPom != null, "root pom.xml with <module>modules/...</module> not found");

    Matcher matcher =
        Pattern.compile("<module>modules/([^<]+)</module>").matcher(Files.readString(rootPom));
    Set<String> reactorModules = new TreeSet<>();
    while (matcher.find()) {
      reactorModules.add(matcher.group(1).trim());
    }
    assumeTrue(!reactorModules.isEmpty(), "no modules/* entries found in root pom");

    Set<String> catalogIds =
        StaticModuleCatalog.modules().stream()
            .map(SihsalusModuleDescriptor::id)
            .collect(Collectors.toCollection(TreeSet::new));

    assertEquals(
        reactorModules,
        catalogIds,
        "Maven reactor modules/* must exactly match StaticModuleCatalog ids");
  }

  private static Path locateRootPom() throws IOException {
    for (Path candidate = Paths.get("").toAbsolutePath();
        candidate != null;
        candidate = candidate.getParent()) {
      Path pom = candidate.resolve("pom.xml");
      if (Files.isRegularFile(pom) && Files.readString(pom).contains("<module>modules/")) {
        return pom;
      }
    }
    return null;
  }

  private static void assertNotBlank(String value, String field) {
    assertNotNull(value, field + " must not be null");
    assertFalse(value.isBlank(), field + " must not be blank");
  }
}
