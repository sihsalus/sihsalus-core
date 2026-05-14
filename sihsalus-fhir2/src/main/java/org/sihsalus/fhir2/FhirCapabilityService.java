package org.sihsalus.fhir2;

import java.lang.annotation.Annotation;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.Delete;
import ca.uhn.fhir.rest.annotation.Patch;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Update;
import ca.uhn.fhir.rest.server.IResourceProvider;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestComponent;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestResourceComponent;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementKind;
import org.hl7.fhir.r4.model.CapabilityStatement.ResourceInteractionComponent;
import org.hl7.fhir.r4.model.CapabilityStatement.RestfulCapabilityMode;
import org.hl7.fhir.r4.model.CapabilityStatement.TypeRestfulInteraction;
import org.hl7.fhir.r4.model.Enumerations.FHIRVersion;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.openmrs.module.fhir2.api.annotations.R4Provider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class FhirCapabilityService {

    private final FhirContext fhirContext;

    private final java.util.List<IResourceProvider> providers;

    public FhirCapabilityService(
            @Qualifier("fhirR4") FhirContext fhirContext, @R4Provider java.util.List<IResourceProvider> providers) {
        this.fhirContext = fhirContext;
        this.providers = new ArrayList<>(providers);
    }

    public String capabilityStatementJson() {
        return fhirContext.newJsonParser().encodeResourceToString(capabilityStatement());
    }

    CapabilityStatement capabilityStatement() {
        CapabilityStatement statement = new CapabilityStatement();
        statement.setStatus(PublicationStatus.ACTIVE);
        statement.setDate(Date.from(OffsetDateTime.now().toInstant()));
        statement.setPublisher("SIH Salus");
        statement.setKind(CapabilityStatementKind.INSTANCE);
        statement.setFhirVersion(FHIRVersion._4_0_1);
        statement.addFormat("json");

        CapabilityStatementRestComponent rest = statement.addRest();
        rest.setMode(RestfulCapabilityMode.SERVER);

        providerCapabilities().forEach((resourceType, interactions) -> {
            CapabilityStatementRestResourceComponent resource = rest.addResource();
            resource.setType(resourceType);
            interactions.forEach(interaction -> resource.addInteraction(new ResourceInteractionComponent().setCode(interaction)));
        });

        return statement;
    }

    private Map<String, Set<TypeRestfulInteraction>> providerCapabilities() {
        Map<String, Set<TypeRestfulInteraction>> resources = new LinkedHashMap<>();
        providers.stream()
                .sorted(Comparator.comparing(provider -> resourceType(provider.getResourceType())))
                .forEach(provider -> resources
                        .computeIfAbsent(providerResourceType(provider), ignored -> new LinkedHashSet<>())
                        .addAll(providerInteractions(provider)));
        return resources;
    }

    private String providerResourceType(IResourceProvider provider) {
        return resourceType(provider.getResourceType());
    }

    private String resourceType(Class<?> resourceClass) {
        return fhirContext.getResourceType(resourceClass.asSubclass(org.hl7.fhir.instance.model.api.IBaseResource.class));
    }

    private Set<TypeRestfulInteraction> providerInteractions(IResourceProvider provider) {
        Set<TypeRestfulInteraction> interactions = new LinkedHashSet<>();
        addInteractionIfPresent(provider, interactions, Read.class, TypeRestfulInteraction.READ);
        addInteractionIfPresent(provider, interactions, Search.class, TypeRestfulInteraction.SEARCHTYPE);
        addInteractionIfPresent(provider, interactions, Create.class, TypeRestfulInteraction.CREATE);
        addInteractionIfPresent(provider, interactions, Update.class, TypeRestfulInteraction.UPDATE);
        addInteractionIfPresent(provider, interactions, Patch.class, TypeRestfulInteraction.PATCH);
        addInteractionIfPresent(provider, interactions, Delete.class, TypeRestfulInteraction.DELETE);
        return interactions;
    }

    private void addInteractionIfPresent(
            IResourceProvider provider,
            Set<TypeRestfulInteraction> interactions,
            Class<? extends Annotation> annotation,
            TypeRestfulInteraction interaction) {
        for (java.lang.reflect.Method method : provider.getClass().getMethods()) {
            if (method.isAnnotationPresent(annotation)) {
                interactions.add(interaction);
                return;
            }
        }
    }
}
