package org.sihsalus.core.boot;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.sihsalus.core.api.SihsalusModuleDescriptor;
import org.sihsalus.initializer.SihsalusContentPaths;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
final class StaticModuleRuntimeInspector {

  private static final Map<String, String> CONFIGURATION_BEANS =
      Map.ofEntries(
          Map.entry("initializer", "sihsalusInitializerConfiguration"),
          Map.entry("fhir2", "fhir2Configuration"),
          Map.entry("webservices-rest", "systemRestConfiguration"),
          Map.entry("authentication", "sihsalusAuthenticationConfiguration"),
          Map.entry("oauth2login", "sihsalusOAuth2LoginConfiguration"),
          Map.entry("idgen", "sihsalusIdgenConfiguration"),
          Map.entry("addresshierarchy", "sihsalusAddressHierarchyConfiguration"),
          Map.entry("emrapi", "sihsalusEmrApiConfiguration"),
          Map.entry("o3forms", "sihsalusO3FormsConfiguration"),
          Map.entry("reporting", "sihsalusReportingConfiguration"),
          Map.entry("billing", "sihsalusBillingConfiguration"),
          Map.entry("stockmanagement", "sihsalusStockManagementConfiguration"),
          Map.entry("datafilter", "sihsalusDataFilterConfiguration"),
          Map.entry("fua", "sihsalusFuaConfiguration"),
          Map.entry("imaging", "sihsalusImagingConfiguration"),
          Map.entry("attachments", "sihsalusAttachmentsConfiguration"),
          Map.entry("patientdocuments", "sihsalusPatientDocumentsConfiguration"),
          Map.entry("cohort", "sihsalusCohortConfiguration"),
          Map.entry("queue", "sihsalusQueueConfiguration"),
          Map.entry("appointments", "sihsalusAppointmentsConfiguration"),
          Map.entry("teleconsultation", "sihsalusTeleconsultationConfiguration"),
          Map.entry("bedmanagement", "sihsalusBedManagementConfiguration"),
          Map.entry("metadatamapping", "sihsalusMetadataMappingConfiguration"),
          Map.entry("openconceptlab", "sihsalusOpenConceptLabConfiguration"),
          Map.entry("sihsalusinterop", "sihsalusInteropConfiguration"),
          Map.entry("event", "sihsalusEventConfiguration"),
          Map.entry("calculation", "sihsalusCalculationConfiguration"),
          Map.entry("htmlwidgets", "sihsalusHtmlWidgetsConfiguration"),
          Map.entry("reportingrest", "sihsalusReportingRestConfiguration"),
          Map.entry("serialization-xstream", "sihsalusSerializationXstreamConfiguration"),
          Map.entry("ordertemplates", "sihsalusOrderTemplatesConfiguration"),
          Map.entry("patientflags", "sihsalusPatientFlagsConfiguration"),
          Map.entry("legacyui", "sihsalusLegacyUiConfiguration"));

  private static final Map<String, String> LIQUIBASE_FILES =
      Map.ofEntries(
          Map.entry("fhir2", "org/openmrs/module/fhir2/liquibase.xml"),
          Map.entry("idgen", "org/openmrs/module/idgen/liquibase.xml"),
          Map.entry("addresshierarchy", "org/openmrs/module/addresshierarchy/liquibase.xml"),
          Map.entry("metadatamapping", "org/openmrs/module/metadatamapping/liquibase.xml"),
          Map.entry("o3forms", "org/openmrs/module/o3forms/liquibase.xml"),
          Map.entry("calculation", "org/openmrs/calculation/liquibase.xml"),
          Map.entry("emrapi", "org/openmrs/module/emrapi/liquibase.xml"),
          Map.entry("attachments", "org/openmrs/module/attachments/liquibase.xml"),
          Map.entry("cohort", "org/openmrs/module/cohort/liquibase.xml"),
          Map.entry("queue", "org/openmrs/module/queue/liquibase.xml"),
          Map.entry("appointments", "org/openmrs/module/appointments/liquibase.xml"),
          Map.entry("bedmanagement", "org/openmrs/module/bedmanagement/liquibase.xml"),
          Map.entry("openconceptlab", "org/openmrs/module/openconceptlab/liquibase.xml"),
          Map.entry("teleconsultation", "org/bahmni/module/teleconsultation/liquibase.xml"),
          Map.entry("sihsalusinterop", "org/openmrs/module/sihsalusinterop/liquibase.xml"),
          Map.entry("reporting", "org/openmrs/module/reporting/liquibase.xml"),
          Map.entry("stockmanagement", "org/openmrs/module/stockmanagement/liquibase.xml"),
          Map.entry("datafilter", "org/openmrs/module/datafilter/liquibase.xml"),
          Map.entry("billing", "org/openmrs/module/billing/liquibase.xml"),
          Map.entry("fua", "org/openmrs/module/fua/liquibase.xml"),
          Map.entry("imaging", "org/openmrs/module/imaging/liquibase.xml"),
          Map.entry("patientdocuments", "org/openmrs/module/patientdocuments/liquibase.xml"),
          Map.entry("ordertemplates", "org/openmrs/module/ordertemplates/liquibase.xml"),
          Map.entry("patientflags", "org/openmrs/module/patientflags/liquibase.xml"),
          Map.entry("legacyui", "org/openmrs/module/legacyui/liquibase.xml"));

  private static final Map<String, String> SCHEDULER_CLASS_PREFIXES =
      Map.ofEntries(
          Map.entry("addresshierarchy", "org.openmrs.module.addresshierarchy."),
          Map.entry("appointments", "org.openmrs.module.appointments."),
          Map.entry("billing", "org.openmrs.module.billing."),
          Map.entry("queue", "org.openmrs.module.queue."),
          Map.entry("sihsalusinterop", "org.openmrs.module.sihsalusinterop."),
          Map.entry("stockmanagement", "org.openmrs.module.stockmanagement."));

  private final ApplicationContext applicationContext;

  private final JdbcTemplate jdbcTemplate;

  private final Environment environment;

  StaticModuleRuntimeInspector(
      ApplicationContext applicationContext, JdbcTemplate jdbcTemplate, Environment environment) {
    this.applicationContext = applicationContext;
    this.jdbcTemplate = jdbcTemplate;
    this.environment = environment;
  }

  StaticModuleRuntimeState inspect(SihsalusModuleDescriptor module) {
    String moduleId = module.id();
    boolean configured =
        environment.getProperty("sihsalus.modules." + moduleId + ".enabled", Boolean.class, true);
    boolean springRegistered = springRegistered(moduleId);
    boolean databaseManaged = LIQUIBASE_FILES.containsKey(moduleId);
    boolean databaseMigrated = databaseManaged && databaseMigrated(moduleId);
    int activeScheduledTasks = activeScheduledTasks(moduleId);

    Map<String, Object> details = new LinkedHashMap<>();
    details.put("lifecycle", lifecycle(configured, springRegistered));
    details.put("configurationBean", CONFIGURATION_BEANS.getOrDefault(moduleId, ""));
    details.put("legacySchedulerControlled", SCHEDULER_CLASS_PREFIXES.containsKey(moduleId));
    addContentDetails(moduleId, details);

    return new StaticModuleRuntimeState(
        true,
        configured,
        springRegistered,
        configured && springRegistered,
        databaseManaged,
        databaseMigrated,
        activeScheduledTasks,
        details);
  }

  private boolean springRegistered(String moduleId) {
    String beanName = CONFIGURATION_BEANS.get(moduleId);
    return beanName != null && applicationContext.containsBean(beanName);
  }

  private boolean databaseMigrated(String moduleId) {
    try {
      Integer count =
          jdbcTemplate.queryForObject(
              "select count(*) from liquibasechangelog where filename = ?",
              Integer.class,
              LIQUIBASE_FILES.get(moduleId));
      return count != null && count > 0;
    } catch (DataAccessException e) {
      return false;
    }
  }

  private int activeScheduledTasks(String moduleId) {
    String classPrefix = SCHEDULER_CLASS_PREFIXES.get(moduleId);
    if (classPrefix == null) {
      return 0;
    }
    try {
      Integer count =
          jdbcTemplate.queryForObject(
              "select count(*) from scheduler_task_config "
                  + "where schedulable_class like ? and (started = true or start_on_startup = true)",
              Integer.class,
              classPrefix + "%");
      return count == null ? 0 : count;
    } catch (DataAccessException e) {
      return 0;
    }
  }

  private String lifecycle(boolean configured, boolean springRegistered) {
    if (!configured) {
      return "disabled";
    }
    if (!springRegistered) {
      return "compiled_only";
    }
    return "started";
  }

  private void addContentDetails(String moduleId, Map<String, Object> details) {
    if ("initializer".equals(moduleId)) {
      details.put(
          "startupLoad",
          environment.getProperty("sihsalus.initializer.startup-load", "continue_on_error"));
      details.put("sourceRoot", SihsalusContentPaths.resolveSourceRoot().toString());
      details.put("configurationRootAvailable", configurationRootAvailable());
    }
    if ("openconceptlab".equals(moduleId)) {
      details.put(
          "staticImportEnabled",
          environment.getProperty("sihsalus.ocl.static-import.enabled", Boolean.class, true));
      details.put(
          "staticImportFailOnErrors",
          environment.getProperty(
              "sihsalus.ocl.static-import.fail-on-errors", Boolean.class, true));
      details.put("oclContentDirectoryAvailable", oclContentDirectoryAvailable());
    }
  }

  private boolean configurationRootAvailable() {
    try {
      return SihsalusContentPaths.resolveConfigRoot() != null;
    } catch (Exception e) {
      return false;
    }
  }

  private boolean oclContentDirectoryAvailable() {
    try {
      Path configRoot = SihsalusContentPaths.resolveConfigRoot();
      return configRoot != null && Files.isDirectory(configRoot.resolve("ocl"));
    } catch (Exception e) {
      return false;
    }
  }
}
