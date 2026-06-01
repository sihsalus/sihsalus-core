package org.sihsalus.core.api;

/**
 * Self-describing metadata for a static (compiled-in) module. Keeping the configuration bean name,
 * the Liquibase changelog path and the legacy scheduler class prefix on the descriptor itself makes
 * {@link StaticModuleCatalog} the single source of truth, instead of duplicating this information
 * in parallel maps elsewhere (which could silently drift out of sync).
 *
 * @param id stable short identifier (matches {@code sihsalus.modules.<id>.enabled} and the {@code
 *     modules/<id>} directory)
 * @param sourceModule upstream OpenMRS artifact this module was imported from
 * @param baselineVersion upstream version the import was based on (provenance, not the Maven
 *     artifact version)
 * @param status lifecycle classification of the module
 * @param configBean name of the Spring {@code @Configuration} bean that wires the module (never
 *     blank)
 * @param liquibaseFile classpath of the module's Liquibase changelog, or {@code null} if the module
 *     is not database-managed
 * @param schedulerPrefix class-name prefix used by the module's legacy scheduler tasks, or {@code
 *     null} if the module has none
 */
public record SihsalusModuleDescriptor(
    String id,
    String sourceModule,
    String baselineVersion,
    SihsalusModuleStatus status,
    String configBean,
    String liquibaseFile,
    String schedulerPrefix) {

  public boolean isDatabaseManaged() {
    return liquibaseFile != null;
  }

  public boolean hasLegacyScheduler() {
    return schedulerPrefix != null;
  }
}
