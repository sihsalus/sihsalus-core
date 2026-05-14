package org.sihsalus.core.api;

import java.util.List;

public final class StaticModuleCatalog {

    private static final List<SihsalusModuleDescriptor> MODULES =
            List.of(
                    module("initializer", "initializer-omod", "2.11.0", SihsalusModuleStatus.STATIC_INTERNAL),
                    module("fhir2", "fhir2-omod", "4.0.0-SNAPSHOT", SihsalusModuleStatus.STATIC_INTERNAL),
                    module("webservices-rest", "webservices.rest-omod", "3.4.1", SihsalusModuleStatus.STATIC_INTERNAL),
                    module("authentication", "authentication-omod", "2.3.0", SihsalusModuleStatus.PLACEHOLDER),
                    module("oauth2login", "oauth2login-omod", "1.5.0", SihsalusModuleStatus.PLACEHOLDER),
                    module("idgen", "idgen-api", "6.0.0-SNAPSHOT", SihsalusModuleStatus.STATIC_INTERNAL),
                    module("addresshierarchy", "addresshierarchy-omod", "2.21.0", SihsalusModuleStatus.PLACEHOLDER),
                    module("patientdocuments", "patientdocuments-omod", "1.1.0-SNAPSHOT", SihsalusModuleStatus.PLACEHOLDER),
                    module("attachments", "attachments-omod", "4.0.0", SihsalusModuleStatus.PLACEHOLDER),
                    module("cohort", "cohort-omod", "3.7.3", SihsalusModuleStatus.PLACEHOLDER),
                    module("patientflags", "patientflags-omod", "3.0.10", SihsalusModuleStatus.PLACEHOLDER),
                    module("o3forms", "o3forms-omod", "2.3.0", SihsalusModuleStatus.PLACEHOLDER),
                    module("emrapi", "emrapi-omod", "3.4.0", SihsalusModuleStatus.PLACEHOLDER),
                    module("queue", "queue-omod", "3.0.0", SihsalusModuleStatus.PLACEHOLDER),
                    module("appointments", "appointments-omod", "2.1.0-20250318.070530-1", SihsalusModuleStatus.PLACEHOLDER),
                    module("teleconsultation", "teleconsultation-omod", "2.1.0-20250318.154145-1", SihsalusModuleStatus.PLACEHOLDER),
                    module("bedmanagement", "bedmanagement-omod", "7.2.0", SihsalusModuleStatus.PLACEHOLDER),
                    module("reporting", "reporting-omod", "2.1.0", SihsalusModuleStatus.PLACEHOLDER),
                    module("reportingrest", "reportingrest-omod", "2.0.0", SihsalusModuleStatus.PLACEHOLDER),
                    module("calculation", "calculation-omod", "2.0.0", SihsalusModuleStatus.PLACEHOLDER),
                    module("htmlwidgets", "htmlwidgets-omod", "2.0.1", SihsalusModuleStatus.PLACEHOLDER),
                    module("serialization-xstream", "serialization.xstream-api", "0.3.0", SihsalusModuleStatus.PLACEHOLDER),
                    module("metadatamapping", "metadatamapping-omod", "2.0.0", SihsalusModuleStatus.PLACEHOLDER),
                    module("openconceptlab", "openconceptlab-omod", "3.0.0", SihsalusModuleStatus.PLACEHOLDER),
                    module("ordertemplates", "ordertemplates-omod", "2.2.0", SihsalusModuleStatus.PLACEHOLDER),
                    module("event", "event-omod", "4.0.0", SihsalusModuleStatus.PLACEHOLDER),
                    module("stockmanagement", "stockmanagement-omod", "3.0.0", SihsalusModuleStatus.PLACEHOLDER),
                    module("billing", "billing-omod", "2.2.0", SihsalusModuleStatus.PLACEHOLDER),
                    module("fua", "fua-omod", "1.0.75", SihsalusModuleStatus.PLACEHOLDER),
                    module("imaging", "imaging-omod", "1.2.2", SihsalusModuleStatus.PLACEHOLDER),
                    module("legacyui", "legacyui-omod", "2.1.0", SihsalusModuleStatus.PLACEHOLDER));

    private StaticModuleCatalog() {}

    public static List<SihsalusModuleDescriptor> modules() {
        return MODULES;
    }

    private static SihsalusModuleDescriptor module(
            String id, String sourceModule, String baselineVersion, SihsalusModuleStatus status) {
        return new SihsalusModuleDescriptor(id, sourceModule, baselineVersion, status);
    }
}
