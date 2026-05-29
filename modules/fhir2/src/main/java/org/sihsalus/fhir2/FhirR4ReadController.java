package org.sihsalus.fhir2;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.IQueryParameterAnd;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.annotation.IncludeParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Sort;
import ca.uhn.fhir.rest.api.QualifiedParamList;
import ca.uhn.fhir.rest.api.SortOrderEnum;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Resource;
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.ContextAuthenticationException;
import org.openmrs.module.fhir2.api.annotations.R4Provider;
import org.sihsalus.core.api.authorization.PatientObjectAuthorizationService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FhirR4ReadController {

  private static final MediaType FHIR_JSON = MediaType.parseMediaType("application/fhir+json");

  private static final int DEFAULT_SEARCH_COUNT = 10;

  private static final int MAX_SEARCH_COUNT = 50;

  private static final Set<String> COMMON_SEARCH_PARAMETERS =
      Set.of("_count", "_format", "_pretty", "_summary", "_tag");

  private final FhirContext fhirContext;

  private final Map<String, IResourceProvider> providersByResourceType;

  private final PatientObjectAuthorizationService patientAuthorization;

  @Autowired
  FhirR4ReadController(
      @Qualifier("fhirR4") FhirContext fhirContext,
      @R4Provider List<IResourceProvider> providers,
      ObjectProvider<PatientObjectAuthorizationService> patientAuthorization) {
    this(
        fhirContext,
        providers,
        patientAuthorization.getIfAvailable(PatientObjectAuthorizationService::permitAll));
  }

  FhirR4ReadController(
      @Qualifier("fhirR4") FhirContext fhirContext, @R4Provider List<IResourceProvider> providers) {
    this(fhirContext, providers, PatientObjectAuthorizationService.permitAll());
  }

  private FhirR4ReadController(
      FhirContext fhirContext,
      List<IResourceProvider> providers,
      PatientObjectAuthorizationService patientAuthorization) {
    this.fhirContext = fhirContext;
    this.providersByResourceType = new LinkedHashMap<>();
    this.patientAuthorization = patientAuthorization;
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
  ResponseEntity<String> read(
      @PathVariable("resourceType") String resourceType, @PathVariable("id") String id) {
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
      if (isPatientResource(resourceType) && !patientAuthorization.canReadPatient(id)) {
        return patientAccessDenied(id);
      }
      IBaseResource resource = invokeRead(provider, readMethod, id);
      if (resource == null) {
        return operationOutcome(
            404,
            "FHIR R4 resource was not found: " + resourceType + "/" + id,
            OperationOutcome.IssueType.NOTFOUND);
      }
      return fhirResponse(200, fhirContext.newJsonParser().encodeResourceToString(resource));
    } catch (BaseServerResponseException exception) {
      return operationOutcome(exception);
    } catch (APIAuthenticationException exception) {
      return operationOutcome(
          authenticationFailureStatus(),
          exception.getMessage(),
          OperationOutcome.IssueType.FORBIDDEN);
    } catch (ContextAuthenticationException exception) {
      return operationOutcome(401, exception.getMessage(), OperationOutcome.IssueType.FORBIDDEN);
    }
  }

  @GetMapping(
      value = {
        "/api/fhir/{resourceType}",
        "/api/fhir/r4/{resourceType}",
        "/ws/fhir2/R4/{resourceType}"
      },
      produces = {"application/fhir+json", "application/json"})
  ResponseEntity<String> search(
      @PathVariable("resourceType") String resourceType,
      @RequestParam MultiValueMap<String, String> queryParameters) {
    IResourceProvider provider = providersByResourceType.get(resourceType);
    if (provider == null) {
      return operationOutcome(
          404,
          "FHIR R4 resource type is not registered: " + resourceType,
          OperationOutcome.IssueType.NOTFOUND);
    }

    Method searchMethod = searchMethod(provider);
    if (searchMethod == null) {
      return operationOutcome(
          405,
          "FHIR R4 search is not supported for resource type: " + resourceType,
          OperationOutcome.IssueType.NOTSUPPORTED);
    }

    try {
      rejectUnsupportedSearchParameters(searchMethod, queryParameters);
      String deniedPatientUuid = deniedPatientSearchReference(queryParameters);
      if (deniedPatientUuid != null) {
        return patientAccessDenied(deniedPatientUuid);
      }
      IBundleProvider bundleProvider = invokeSearch(provider, searchMethod, queryParameters);
      Bundle bundle = bundleFromProvider(resourceType, bundleProvider, queryParameters);
      return fhirResponse(200, fhirContext.newJsonParser().encodeResourceToString(bundle));
    } catch (IllegalArgumentException exception) {
      return operationOutcome(400, exception.getMessage(), OperationOutcome.IssueType.INVALID);
    } catch (BaseServerResponseException exception) {
      return operationOutcome(exception);
    } catch (APIAuthenticationException exception) {
      return operationOutcome(
          authenticationFailureStatus(),
          exception.getMessage(),
          OperationOutcome.IssueType.FORBIDDEN);
    } catch (ContextAuthenticationException exception) {
      return operationOutcome(401, exception.getMessage(), OperationOutcome.IssueType.FORBIDDEN);
    }
  }

  private int authenticationFailureStatus() {
    return Context.isSessionOpen() && Context.isAuthenticated() ? 403 : 401;
  }

  private boolean isPatientResource(String resourceType) {
    return "Patient".equals(resourceType);
  }

  private ResponseEntity<String> patientAccessDenied(String patientUuid) {
    return operationOutcome(
        403,
        "Patient access denied: " + patientUuid,
        OperationOutcome.IssueType.FORBIDDEN);
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

  private Method searchMethod(IResourceProvider provider) {
    for (Method method : provider.getClass().getMethods()) {
      Search search = method.getAnnotation(Search.class);
      if (search != null
          && search.queryName().isBlank()
          && IBundleProvider.class.isAssignableFrom(method.getReturnType())) {
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

  private IBundleProvider invokeSearch(
      IResourceProvider provider, Method method, MultiValueMap<String, String> queryParameters) {
    try {
      Object result = method.invoke(provider, searchArguments(method, queryParameters));
      if (result instanceof IBundleProvider bundleProvider) {
        return bundleProvider;
      }
      throw new IllegalStateException("@Search method returned a non-bundle provider: " + method);
    } catch (IllegalAccessException exception) {
      throw new IllegalStateException(
          "FHIR @Search method is not accessible: " + method, exception);
    } catch (InvocationTargetException exception) {
      Throwable cause = exception.getCause();
      if (cause instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }
      if (cause instanceof Error error) {
        throw error;
      }
      throw new IllegalStateException("FHIR @Search method failed: " + method, cause);
    }
  }

  private Bundle bundleFromProvider(
      String resourceType,
      IBundleProvider bundleProvider,
      MultiValueMap<String, String> queryParameters) {
    Bundle bundle = new Bundle();
    bundle.setType(Bundle.BundleType.SEARCHSET);
    if (bundleProvider == null) {
      return bundle;
    }

    int count = requestedCount(queryParameters);
    List<String> tagFilters = tagFilters(queryParameters);
    Integer total = bundleProvider.size();
    int fetchCount = tagFilters.isEmpty() ? count : MAX_SEARCH_COUNT;
    if (fetchCount == 0) {
      bundle.setTotal(total == null ? 0 : total);
      return bundle;
    }

    List<Resource> resources = new ArrayList<>();
    for (IBaseResource resource : bundleProvider.getResources(0, fetchCount)) {
      if (resource instanceof Resource r4Resource
          && matchesTagFilters(r4Resource, tagFilters)
          && canExposeResource(resourceType, r4Resource)) {
        resources.add(r4Resource);
      }
    }

    bundle.setTotal(tagFilters.isEmpty() && total != null ? total : resources.size());
    for (Resource resource : resources.subList(0, Math.min(count, resources.size()))) {
      bundle.addEntry().setResource(resource);
    }
    return bundle;
  }

  private boolean canExposeResource(String resourceType, Resource resource) {
    if (!isPatientResource(resourceType)) {
      return true;
    }
    String id = resource.getIdElement().getIdPart();
    return id == null || id.isBlank() || patientAuthorization.canReadPatient(id);
  }

  private String deniedPatientSearchReference(MultiValueMap<String, String> queryParameters) {
    for (Map.Entry<String, List<String>> entry : queryParameters.entrySet()) {
      String parameterName = entry.getKey();
      if (!"patient".equals(baseSearchParameterName(parameterName))
          && !"subject".equals(baseSearchParameterName(parameterName))) {
        continue;
      }
      if (parameterName.contains(":") && !parameterName.endsWith(":Patient")) {
        continue;
      }
      for (String value : entry.getValue()) {
        for (String reference : value.split(",")) {
          String patientUuid = patientUuidFromReference(reference);
          if (patientUuid != null && !patientAuthorization.canReadPatient(patientUuid)) {
            return patientUuid;
          }
        }
      }
    }
    return null;
  }

  private String patientUuidFromReference(String reference) {
    if (reference == null || reference.isBlank()) {
      return null;
    }
    String trimmed = reference.trim();
    int tokenSeparator = trimmed.lastIndexOf('|');
    if (tokenSeparator >= 0) {
      trimmed = trimmed.substring(tokenSeparator + 1);
    }
    int slash = trimmed.lastIndexOf('/');
    return slash >= 0 ? trimmed.substring(slash + 1) : trimmed;
  }

  private List<String> tagFilters(MultiValueMap<String, String> queryParameters) {
    List<String> values = queryParameters.get("_tag");
    if (values == null || values.isEmpty()) {
      return List.of();
    }

    List<String> filters = new ArrayList<>();
    for (String value : values) {
      if (value == null || value.isBlank()) {
        throw new IllegalArgumentException("_tag must not be blank");
      }
      filters.add(value);
    }
    return filters;
  }

  private boolean matchesTagFilters(Resource resource, List<String> filters) {
    if (filters.isEmpty()) {
      return true;
    }
    return filters.stream().allMatch(filter -> matchesAnyTagExpression(resource, filter));
  }

  private boolean matchesAnyTagExpression(Resource resource, String filter) {
    for (String expression : filter.split(",")) {
      if (expression.isBlank()) {
        throw new IllegalArgumentException("_tag must not contain blank values");
      }
      if (matchesTagExpression(resource, expression.trim())) {
        return true;
      }
    }
    return false;
  }

  private boolean matchesTagExpression(Resource resource, String expression) {
    String system = null;
    String code = expression;
    int separator = expression.indexOf('|');
    if (separator >= 0) {
      system = expression.substring(0, separator);
      code = expression.substring(separator + 1);
    }

    for (Coding tag : resource.getMeta().getTag()) {
      boolean systemMatches =
          system == null || system.isEmpty() || system.equalsIgnoreCase(tag.getSystem());
      boolean codeMatches =
          code.isEmpty()
              || code.equalsIgnoreCase(tag.getCode())
              || code.equalsIgnoreCase(tag.getDisplay());
      if (systemMatches && codeMatches) {
        return true;
      }
    }
    return false;
  }

  private Object[] searchArguments(Method method, MultiValueMap<String, String> queryParameters) {
    Parameter[] parameters = method.getParameters();
    Object[] arguments = new Object[parameters.length];
    for (int i = 0; i < parameters.length; i++) {
      Parameter parameter = parameters[i];
      OptionalParam optionalParam = parameter.getAnnotation(OptionalParam.class);
      if (optionalParam != null) {
        arguments[i] = optionalParameterArgument(parameter, optionalParam, queryParameters);
        continue;
      }

      if (parameter.isAnnotationPresent(Sort.class)) {
        arguments[i] = sortArgument(queryParameters);
        continue;
      }

      IncludeParam includeParam = parameter.getAnnotation(IncludeParam.class);
      if (includeParam != null) {
        arguments[i] = includeArgument(includeParam, queryParameters);
      }
    }
    return arguments;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private Object optionalParameterArgument(
      Parameter parameter,
      OptionalParam optionalParam,
      MultiValueMap<String, String> queryParameters) {
    List<QueryValue> values = queryValues(optionalParam.name(), queryParameters);
    if (values.isEmpty()) {
      return null;
    }

    Class<?> parameterType = parameter.getType();
    try {
      Object argument = parameterType.getDeclaredConstructor().newInstance();
      if (argument instanceof IQueryParameterAnd andArgument) {
        List<QualifiedParamList> qualifiedValues = new ArrayList<>();
        for (QueryValue value : values) {
          qualifiedValues.addAll(qualifiedParamLists(value.qualifier(), value.value()));
        }
        andArgument.setValuesAsQueryTokens(fhirContext, optionalParam.name(), qualifiedValues);
        return argument;
      }

      if (argument instanceof IQueryParameterType typeArgument) {
        if (values.size() > 1) {
          throw new IllegalArgumentException(
              "FHIR R4 search parameter must be specified only once: " + optionalParam.name());
        }

        QueryValue value = values.get(0);
        typeArgument.setValueAsQueryToken(
            fhirContext, optionalParam.name(), value.qualifier(), value.value());
        return argument;
      }

      throw new IllegalArgumentException(
          "FHIR R4 search parameter type is not supported by this endpoint: "
              + parameterType.getName());
    } catch (ReflectiveOperationException exception) {
      throw new IllegalArgumentException(
          "FHIR R4 search parameter type cannot be created: " + parameterType.getName(), exception);
    }
  }

  private List<QualifiedParamList> qualifiedParamLists(String qualifier, String value) {
    return List.of(QualifiedParamList.splitQueryStringByCommasIgnoreEscape(qualifier, value));
  }

  private SortSpec sortArgument(MultiValueMap<String, String> queryParameters) {
    List<String> values = queryParameters.get("_sort");
    if (values == null || values.isEmpty()) {
      return null;
    }

    SortSpec first = null;
    SortSpec previous = null;
    for (String value : values) {
      for (String rawSort : value.split(",")) {
        if (rawSort.isBlank()) {
          throw new IllegalArgumentException("_sort must not contain blank values");
        }

        String sortParameter = rawSort.trim();
        SortOrderEnum order = SortOrderEnum.ASC;
        if (sortParameter.startsWith("-")) {
          order = SortOrderEnum.DESC;
          sortParameter = sortParameter.substring(1);
        }
        if (sortParameter.isBlank()) {
          throw new IllegalArgumentException("_sort must include a parameter name");
        }

        SortSpec current = new SortSpec(sortParameter, order);
        if (first == null) {
          first = current;
        } else {
          previous.setChain(current);
        }
        previous = current;
      }
    }
    return first;
  }

  private HashSet<Include> includeArgument(
      IncludeParam includeParam, MultiValueMap<String, String> queryParameters) {
    List<String> values = queryParameters.get(includeParam.reverse() ? "_revinclude" : "_include");
    if (values == null || values.isEmpty()) {
      return null;
    }

    HashSet<Include> includes = new HashSet<>();
    for (String value : values) {
      for (String token : value.split(",")) {
        if (token.isBlank()) {
          throw new IllegalArgumentException(
              "FHIR include parameter must not contain blank values");
        }
        includes.add(new Include(token.trim(), includeParam.reverse()));
      }
    }
    return includes;
  }

  private void rejectUnsupportedSearchParameters(
      Method method, MultiValueMap<String, String> queryParameters) {
    Set<String> supportedSearchParameters = supportedSearchParameters(method);
    for (String parameterName : queryParameters.keySet()) {
      if (!supportedSearchParameters.contains(baseSearchParameterName(parameterName))) {
        throw new IllegalArgumentException(
            "FHIR R4 search parameter is not supported by this endpoint: " + parameterName);
      }
    }
  }

  private Set<String> supportedSearchParameters(Method method) {
    Set<String> supported = new LinkedHashSet<>(COMMON_SEARCH_PARAMETERS);
    for (Parameter parameter : method.getParameters()) {
      OptionalParam optionalParam = parameter.getAnnotation(OptionalParam.class);
      if (optionalParam != null) {
        supported.add(optionalParam.name());
      }

      if (parameter.isAnnotationPresent(Sort.class)) {
        supported.add("_sort");
      }

      IncludeParam includeParam = parameter.getAnnotation(IncludeParam.class);
      if (includeParam != null) {
        supported.add(includeParam.reverse() ? "_revinclude" : "_include");
      }
    }
    return supported;
  }

  private List<QueryValue> queryValues(
      String parameterName, MultiValueMap<String, String> queryParameters) {
    List<QueryValue> values = new ArrayList<>();
    for (Map.Entry<String, List<String>> entry : queryParameters.entrySet()) {
      if (!baseSearchParameterName(entry.getKey()).equals(parameterName)) {
        continue;
      }

      String qualifier = qualifier(entry.getKey(), parameterName);
      for (String value : entry.getValue()) {
        if (value != null && !value.isBlank()) {
          values.add(new QueryValue(qualifier, value));
        }
      }
    }
    return values;
  }

  private String baseSearchParameterName(String parameterName) {
    int qualifierStart = parameterName.indexOf(':');
    return qualifierStart < 0 ? parameterName : parameterName.substring(0, qualifierStart);
  }

  private String qualifier(String qualifiedName, String parameterName) {
    return qualifiedName.length() == parameterName.length()
        ? null
        : qualifiedName.substring(parameterName.length());
  }

  private int requestedCount(MultiValueMap<String, String> queryParameters) {
    String count = singleValue(queryParameters, "_count");
    if (count == null || count.isBlank()) {
      return DEFAULT_SEARCH_COUNT;
    }
    try {
      int parsed = Integer.parseInt(count);
      if (parsed < 0) {
        throw new IllegalArgumentException("_count must be zero or greater");
      }
      return Math.min(parsed, MAX_SEARCH_COUNT);
    } catch (NumberFormatException exception) {
      throw new IllegalArgumentException("_count must be a whole number", exception);
    }
  }

  private String singleValue(MultiValueMap<String, String> queryParameters, String parameterName) {
    List<String> values = queryParameters.get(parameterName);
    if (values == null || values.isEmpty()) {
      return null;
    }
    if (values.size() > 1) {
      throw new IllegalArgumentException(parameterName + " must be specified only once");
    }
    return values.get(0);
  }

  private ResponseEntity<String> operationOutcome(BaseServerResponseException exception) {
    IBaseOperationOutcome outcome = exception.getOperationOutcome();
    if (outcome != null) {
      return fhirResponse(
          exception.getStatusCode(), fhirContext.newJsonParser().encodeResourceToString(outcome));
    }
    return operationOutcome(
        exception.getStatusCode(),
        exception.getMessage(),
        issueTypeForStatus(exception.getStatusCode()));
  }

  private ResponseEntity<String> operationOutcome(
      int status, String diagnostics, OperationOutcome.IssueType issueType) {
    OperationOutcome outcome = new OperationOutcome();
    outcome
        .addIssue()
        .setSeverity(OperationOutcome.IssueSeverity.ERROR)
        .setCode(issueType)
        .setDiagnostics(
            diagnostics == null || diagnostics.isBlank() ? "FHIR request failed" : diagnostics);
    return fhirResponse(status, fhirContext.newJsonParser().encodeResourceToString(outcome));
  }

  private OperationOutcome.IssueType issueTypeForStatus(int status) {
    return status == 404
        ? OperationOutcome.IssueType.NOTFOUND
        : OperationOutcome.IssueType.EXCEPTION;
  }

  private ResponseEntity<String> fhirResponse(int status, String body) {
    return ResponseEntity.status(status)
        .contentType(FHIR_JSON)
        .header(HttpHeaders.CACHE_CONTROL, "no-store")
        .body(body);
  }

  private record QueryValue(String qualifier, String value) {}
}
