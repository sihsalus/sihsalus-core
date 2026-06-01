package org.sihsalus.core.api;

import java.util.List;

/**
 * Single source of truth for the static (compiled-in) modules. Each entry fully describes the
 * module — including its Spring configuration bean, Liquibase changelog and legacy scheduler prefix
 * — so that runtime components (e.g. {@code StaticModuleRuntimeInspector}) can iterate this catalog
 * instead of maintaining parallel lookup tables.
 */
public final class StaticModuleCatalog {

  private static final List<SihsalusModuleDescriptor> MODULES =
      List.of(
          module("initializer", "initializer-omod", "2.11.0", "sihsalusInitializerConfiguration"),
          module(
              "fhir2",
              "fhir2-omod",
              "4.0.0-SNAPSHOT",
              "fhir2Configuration",
              "org/openmrs/module/fhir2/liquibase.xml"),
          module("webservices-rest", "webservices.rest-omod", "3.4.1", "systemRestConfiguration"),
          module(
              "authentication",
              "authentication-api",
              "2.4.0-SNAPSHOT",
              "sihsalusAuthenticationConfiguration"),
          module(
              "oauth2login",
              "oauth2login-api",
              "1.6.0-SNAPSHOT",
              "sihsalusOAuth2LoginConfiguration"),
          module(
              "idgen",
              "idgen-api",
              "6.0.0-SNAPSHOT",
              "sihsalusIdgenConfiguration",
              "org/openmrs/module/idgen/liquibase.xml"),
          module(
              "addresshierarchy",
              "addresshierarchy-api",
              "3.0.0-SNAPSHOT",
              "sihsalusAddressHierarchyConfiguration",
              "org/openmrs/module/addresshierarchy/liquibase.xml",
              "org.openmrs.module.addresshierarchy."),
          module(
              "patientdocuments",
              "patientdocuments-omod",
              "1.1.0-SNAPSHOT",
              "sihsalusPatientDocumentsConfiguration",
              "org/openmrs/module/patientdocuments/liquibase.xml"),
          module(
              "attachments",
              "attachments-omod",
              "4.0.0",
              "sihsalusAttachmentsConfiguration",
              "org/openmrs/module/attachments/liquibase.xml"),
          module(
              "cohort",
              "cohort-omod",
              "3.7.3",
              "sihsalusCohortConfiguration",
              "org/openmrs/module/cohort/liquibase.xml"),
          module(
              "patientflags",
              "patientflags-omod",
              "3.0.10",
              "sihsalusPatientFlagsConfiguration",
              "org/openmrs/module/patientflags/liquibase.xml"),
          module(
              "o3forms",
              "o3forms-omod",
              "2.3.0",
              "sihsalusO3FormsConfiguration",
              "org/openmrs/module/o3forms/liquibase.xml"),
          module(
              "emrapi",
              "emrapi-api",
              "3.5.0-SNAPSHOT",
              "sihsalusEmrApiConfiguration",
              "org/openmrs/module/emrapi/liquibase.xml"),
          module(
              "queue",
              "queue-omod",
              "3.0.0",
              "sihsalusQueueConfiguration",
              "org/openmrs/module/queue/liquibase.xml",
              "org.openmrs.module.queue."),
          module(
              "appointments",
              "appointments-omod",
              "2.1.0-20250318.070530-1",
              "sihsalusAppointmentsConfiguration",
              "org/openmrs/module/appointments/liquibase.xml",
              "org.openmrs.module.appointments."),
          module(
              "teleconsultation",
              "teleconsultation-omod",
              "2.1.0-20250318.154145-1",
              "sihsalusTeleconsultationConfiguration",
              "org/bahmni/module/teleconsultation/liquibase.xml"),
          module(
              "bedmanagement",
              "bedmanagement-omod",
              "7.2.0",
              "sihsalusBedManagementConfiguration",
              "org/openmrs/module/bedmanagement/liquibase.xml"),
          module(
              "reporting",
              "reporting-omod",
              "2.1.0",
              "sihsalusReportingConfiguration",
              "org/openmrs/module/reporting/liquibase.xml"),
          module(
              "reportingrest", "reportingrest-omod", "2.0.0", "sihsalusReportingRestConfiguration"),
          module(
              "calculation",
              "calculation-api",
              "2.1.0-SNAPSHOT",
              "sihsalusCalculationConfiguration",
              "org/openmrs/calculation/liquibase.xml"),
          module("htmlwidgets", "htmlwidgets-omod", "2.0.1", "sihsalusHtmlWidgetsConfiguration"),
          module(
              "serialization-xstream",
              "serialization.xstream-api",
              "0.3.0",
              "sihsalusSerializationXstreamConfiguration"),
          module(
              "metadatamapping",
              "metadatamapping-api",
              "2.1.0-SNAPSHOT",
              "sihsalusMetadataMappingConfiguration",
              "org/openmrs/module/metadatamapping/liquibase.xml"),
          module(
              "openconceptlab",
              "openconceptlab-omod",
              "3.0.0",
              "sihsalusOpenConceptLabConfiguration",
              "org/openmrs/module/openconceptlab/liquibase.xml"),
          module(
              "sihsalusinterop",
              "sihsalusinterop-omod",
              "1.0.3",
              "sihsalusInteropConfiguration",
              "org/openmrs/module/sihsalusinterop/liquibase.xml",
              "org.openmrs.module.sihsalusinterop."),
          module(
              "ordertemplates",
              "ordertemplates-omod",
              "2.2.0",
              "sihsalusOrderTemplatesConfiguration",
              "org/openmrs/module/ordertemplates/liquibase.xml"),
          module("event", "event-omod", "4.0.0", "sihsalusEventConfiguration"),
          module(
              "stockmanagement",
              "stockmanagement-api",
              "3.0.0",
              "sihsalusStockManagementConfiguration",
              "org/openmrs/module/stockmanagement/liquibase.xml",
              "org.openmrs.module.stockmanagement."),
          module(
              "datafilter",
              "datafilter-omod",
              "0.1.0-SNAPSHOT",
              "sihsalusDataFilterConfiguration",
              "org/openmrs/module/datafilter/liquibase.xml"),
          module(
              "billing",
              "billing-omod",
              "2.3.0-SNAPSHOT",
              "sihsalusBillingConfiguration",
              "org/openmrs/module/billing/liquibase.xml",
              "org.openmrs.module.billing."),
          module(
              "fua",
              "fua-omod",
              "1.0.75",
              "sihsalusFuaConfiguration",
              "org/openmrs/module/fua/liquibase.xml"),
          module(
              "imaging",
              "imaging-omod",
              "1.2.2",
              "sihsalusImagingConfiguration",
              "org/openmrs/module/imaging/liquibase.xml"),
          module(
              "legacyui",
              "legacyui-omod",
              "2.1.0",
              "sihsalusLegacyUiConfiguration",
              "org/openmrs/module/legacyui/liquibase.xml"));

  private StaticModuleCatalog() {}

  public static List<SihsalusModuleDescriptor> modules() {
    return MODULES;
  }

  public static List<SihsalusModuleDescriptor> staticInternalModules() {
    return MODULES.stream()
        .filter(module -> module.status() == SihsalusModuleStatus.STATIC_INTERNAL)
        .toList();
  }

  private static SihsalusModuleDescriptor module(
      String id, String sourceModule, String baselineVersion, String configBean) {
    return module(id, sourceModule, baselineVersion, configBean, null, null);
  }

  private static SihsalusModuleDescriptor module(
      String id,
      String sourceModule,
      String baselineVersion,
      String configBean,
      String liquibaseFile) {
    return module(id, sourceModule, baselineVersion, configBean, liquibaseFile, null);
  }

  private static SihsalusModuleDescriptor module(
      String id,
      String sourceModule,
      String baselineVersion,
      String configBean,
      String liquibaseFile,
      String schedulerPrefix) {
    return new SihsalusModuleDescriptor(
        id,
        sourceModule,
        baselineVersion,
        SihsalusModuleStatus.STATIC_INTERNAL,
        configBean,
        liquibaseFile,
        schedulerPrefix);
  }
}
