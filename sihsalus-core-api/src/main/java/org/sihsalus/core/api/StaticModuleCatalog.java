package org.sihsalus.core.api;

import java.util.List;

public final class StaticModuleCatalog {

    private static final List<SihsalusModuleDescriptor> MODULES =
            List.of(
                    module("initializer", "initializer-omod", "2.11.0", SihsalusModuleStatus.STATIC_INTERNAL),
                    module("fhir2", "fhir2-omod", "4.0.0-SNAPSHOT", SihsalusModuleStatus.STATIC_INTERNAL),
                    module("webservices-rest", "webservices.rest-omod", "3.4.1", SihsalusModuleStatus.STATIC_INTERNAL),
                    module("authentication", "authentication-api", "2.4.0-SNAPSHOT", SihsalusModuleStatus.STATIC_INTERNAL),
                    module("oauth2login", "oauth2login-api", "1.6.0-SNAPSHOT", SihsalusModuleStatus.STATIC_INTERNAL),
                    module("idgen", "idgen-api", "6.0.0-SNAPSHOT", SihsalusModuleStatus.STATIC_INTERNAL),
                    module("addresshierarchy", "addresshierarchy-api", "3.0.0-SNAPSHOT", SihsalusModuleStatus.STATIC_INTERNAL),
                    module(
                            "patientdocuments",
                            "patientdocuments-omod",
                            "1.1.0-SNAPSHOT",
                            SihsalusModuleStatus.STATIC_INTERNAL),
                    module("attachments", "attachments-omod", "4.0.0", SihsalusModuleStatus.STATIC_INTERNAL),
                    module("cohort", "cohort-omod", "3.7.3", SihsalusModuleStatus.STATIC_INTERNAL),
                    module("patientflags", "patientflags-omod", "3.0.10", SihsalusModuleStatus.PLACEHOLDER),
                    module("o3forms", "o3forms-omod", "2.3.0", SihsalusModuleStatus.STATIC_INTERNAL),
                    module("emrapi", "emrapi-api", "3.5.0-SNAPSHOT", SihsalusModuleStatus.STATIC_INTERNAL),
                    module("queue", "queue-omod", "3.0.0", SihsalusModuleStatus.PLACEHOLDER),
                    module("appointments", "appointments-omod", "2.1.0-20250318.070530-1", SihsalusModuleStatus.PLACEHOLDER),
                    module("teleconsultation", "teleconsultation-omod", "2.1.0-20250318.154145-1", SihsalusModuleStatus.PLACEHOLDER),
                    module("bedmanagement", "bedmanagement-omod", "7.2.0", SihsalusModuleStatus.PLACEHOLDER),
                    module("reporting", "reporting-omod", "2.1.0", SihsalusModuleStatus.STATIC_INTERNAL),
                    module("reportingrest", "reportingrest-omod", "2.0.0", SihsalusModuleStatus.STATIC_INTERNAL),
                    module("calculation", "calculation-api", "2.1.0-SNAPSHOT", SihsalusModuleStatus.STATIC_INTERNAL),
                    module("htmlwidgets", "htmlwidgets-omod", "2.0.1", SihsalusModuleStatus.STATIC_INTERNAL),
                    module(
                            "serialization-xstream",
                            "serialization.xstream-api",
                            "0.3.0",
                            SihsalusModuleStatus.STATIC_INTERNAL),
                    module(
                            "metadatamapping",
                            "metadatamapping-api",
                            "2.1.0-SNAPSHOT",
                            SihsalusModuleStatus.STATIC_INTERNAL),
                    module("openconceptlab", "openconceptlab-omod", "3.0.0", SihsalusModuleStatus.PLACEHOLDER),
                    module("ordertemplates", "ordertemplates-omod", "2.2.0", SihsalusModuleStatus.STATIC_INTERNAL),
                    module("event", "event-omod", "4.0.0", SihsalusModuleStatus.PLACEHOLDER),
                    module("stockmanagement", "stockmanagement-api", "3.0.0", SihsalusModuleStatus.STATIC_INTERNAL),
                    module("billing", "billing-omod", "2.2.0", SihsalusModuleStatus.PLACEHOLDER),
                    module("fua", "fua-omod", "1.0.75", SihsalusModuleStatus.STATIC_INTERNAL),
                    module("imaging", "imaging-omod", "1.2.2", SihsalusModuleStatus.PLACEHOLDER),
                    module("legacyui", "legacyui-omod", "2.1.0", SihsalusModuleStatus.STATIC_INTERNAL));

    private StaticModuleCatalog() {}

    public static List<SihsalusModuleDescriptor> modules() {
        return MODULES;
    }

    private static SihsalusModuleDescriptor module(
            String id, String sourceModule, String baselineVersion, SihsalusModuleStatus status) {
        return new SihsalusModuleDescriptor(id, sourceModule, baselineVersion, status);
    }
}
