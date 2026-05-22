package org.sihsalus.fhir2;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.context.Context;
import org.openmrs.module.fhir2.api.annotations.R4Provider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FhirR4ReadController {

    private static final MediaType FHIR_JSON = MediaType.parseMediaType("application/fhir+json");

    private final FhirContext fhirContext;

    private final Map<String, IResourceProvider> providersByResourceType;

    FhirR4ReadController(@Qualifier("fhirR4") FhirContext fhirContext, @R4Provider List<IResourceProvider> providers) {
        this.fhirContext = fhirContext;
        this.providersByResourceType = new LinkedHashMap<>();
        providers.stream()
                .sorted(Comparator.comparing(this::providerResourceType))
                .forEach(provider -> providersByResourceType.put(providerResourceType(provider), provider));
    }

    @GetMapping(
            value = {
                "/api/fhir/{resourceType}/{id}",
                "/api/fhir/r4/{resourceType}/{id}",
                "/ws/fhir2/R4/{resourceType}/{id}"
            },
            produces = {"application/fhir+json", "application/json"})
    ResponseEntity<String> read(@PathVariable("resourceType") String resourceType, @PathVariable("id") String id) {
        IResourceProvider provider = providersByResourceType.get(resourceType);
        if (provider == null) {
            return operationOutcome(
                    404,
                    "FHIR R4 resource type is not registered: " + resourceType,
                    OperationOutcome.IssueType.NOTFOUND);
        }

        Method readMethod = readMethod(provider);
        if (readMethod == null) {
            return operationOutcome(
                    405,
                    "FHIR R4 read is not supported for resource type: " + resourceType,
                    OperationOutcome.IssueType.NOTSUPPORTED);
        }

        try {
            IBaseResource resource = invokeRead(provider, readMethod, id);
            if (resource == null) {
                return operationOutcome(
                        404,
                        "FHIR R4 resource was not found: " + resourceType + "/" + id,
                        OperationOutcome.IssueType.NOTFOUND);
            }
            return fhirResponse(200, fhirContext.newJsonParser().encodeResourceToString(resource));
        } catch (APIAuthenticationException exception) {
            return operationOutcome(
                    Context.isAuthenticated() ? 403 : 401,
                    exception.getMessage(),
                    OperationOutcome.IssueType.FORBIDDEN);
        } catch (BaseServerResponseException exception) {
            return operationOutcome(exception);
        }
    }

    private String providerResourceType(IResourceProvider provider) {
        return fhirContext.getResourceType(provider.getResourceType());
    }

    private Method readMethod(IResourceProvider provider) {
        for (Method method : provider.getClass().getMethods()) {
            if (method.isAnnotationPresent(Read.class)
                    && method.getParameterCount() == 1
                    && method.getParameterTypes()[0].isAssignableFrom(IdType.class)) {
                return method;
            }
        }
        return null;
    }

    private IBaseResource invokeRead(IResourceProvider provider, Method method, String id) {
        try {
            Object result = method.invoke(provider, new IdType(id));
            if (result == null || result instanceof IBaseResource) {
                return (IBaseResource) result;
            }
            throw new IllegalStateException("@Read method returned a non-FHIR resource: " + method);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("FHIR @Read method is not accessible: " + method, exception);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException("FHIR @Read method failed: " + method, cause);
        }
    }

    private ResponseEntity<String> operationOutcome(BaseServerResponseException exception) {
        IBaseOperationOutcome outcome = exception.getOperationOutcome();
        if (outcome != null) {
            return fhirResponse(exception.getStatusCode(), fhirContext.newJsonParser().encodeResourceToString(outcome));
        }
        return operationOutcome(
                exception.getStatusCode(), exception.getMessage(), issueTypeForStatus(exception.getStatusCode()));
    }

    private ResponseEntity<String> operationOutcome(
            int status, String diagnostics, OperationOutcome.IssueType issueType) {
        OperationOutcome outcome = new OperationOutcome();
        outcome.addIssue()
                .setSeverity(OperationOutcome.IssueSeverity.ERROR)
                .setCode(issueType)
                .setDiagnostics(diagnostics == null || diagnostics.isBlank() ? "FHIR request failed" : diagnostics);
        return fhirResponse(status, fhirContext.newJsonParser().encodeResourceToString(outcome));
    }

    private OperationOutcome.IssueType issueTypeForStatus(int status) {
        return status == 404 ? OperationOutcome.IssueType.NOTFOUND : OperationOutcome.IssueType.EXCEPTION;
    }

    private ResponseEntity<String> fhirResponse(int status, String body) {
        return ResponseEntity.status(status)
                .contentType(FHIR_JSON)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(body);
    }
}
