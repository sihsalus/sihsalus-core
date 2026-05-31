package org.sihsalus.module.idgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.openmrs.api.APIException;
import org.openmrs.module.idgen.IdentifierSource;
import org.openmrs.module.idgen.SequentialIdentifierGenerator;
import org.openmrs.module.idgen.service.IdentifierSourceService;
import org.openmrs.module.idgen.validator.IdentifierSourceValidator;
import org.openmrs.module.idgen.validator.RemoteIdentifierSourceValidator;
import org.openmrs.module.idgen.validator.SequentialIdentifierGeneratorValidator;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;

class IdgenIdentifierControllerTest {

  @Test
  void identifierSourceValidatorsUseParameterizedSupportsSignature() throws Exception {
    // Regression for IDE diagnostics: Spring Validator expects Class<?>.
    // A raw Class parameter brings back unchecked and rawtype warnings.
    Class<?>[] validatorTypes = {
      IdentifierSourceValidator.class,
      SequentialIdentifierGeneratorValidator.class,
      RemoteIdentifierSourceValidator.class
    };

    for (Class<?> validatorType : validatorTypes) {
      Method supports = validatorType.getMethod("supports", Class.class);
      Type parameterType = supports.getGenericParameterTypes()[0];

      assertInstanceOf(ParameterizedType.class, parameterType);
      assertEquals("java.lang.Class<?>", parameterType.getTypeName());
    }

    assertTrue(new IdentifierSourceValidator().supports(SequentialIdentifierGenerator.class));
  }

  @Test
  void generateIdentifierSupportsLegacyAndCurrentPaths() {
    // The endpoint mappings are not yet wired by an API call in this module test slice.
    // Assert the annotation explicitly carries the legacy-compatible routes used by clients.
    Method method =
        Arrays.stream(IdgenIdentifierController.class.getDeclaredMethods())
            .filter(m -> "generateIdentifier".equals(m.getName()))
            .findFirst()
            .orElseThrow();
    PostMapping mapping = method.getAnnotation(PostMapping.class);

    assertTrue(
        Arrays.asList(mapping.value())
            .contains("/rest/v1/idgen/identifiersource/{sourceUniqueId}/identifier"));
    assertTrue(
        Arrays.asList(mapping.value())
            .contains("/ws/rest/v1/idgen/identifiersource/{sourceUniqueId}/identifier"));
    assertTrue(
        Arrays.asList(mapping.value()).contains("/rest/v1/idgen/{sourceUniqueId}/identifier"));
    assertTrue(
        Arrays.asList(mapping.value()).contains("/ws/rest/v1/idgen/{sourceUniqueId}/identifier"));
    assertTrue(
        Arrays.asList(mapping.value())
            .contains("/ws/rest/v1/idgen/identifierSource/{sourceUniqueId}/identifier"));
  }

  @Test
  void generateIdentifierPrefersQueryCommentOverBodyComment() {
    IdentifierSource serviceSource = new SequentialIdentifierGenerator();
    serviceSource.setUuid("uuid-123");

    AtomicReference<IdentifierSource> receivedSource = new AtomicReference<>();
    AtomicReference<String> receivedComment = new AtomicReference<>();
    IdentifierSourceService service =
        createServiceStub(
            42,
            serviceSource,
            "uuid-123",
            serviceSource,
            (source, comment) -> {
              receivedSource.set(source);
              receivedComment.set(comment);
              return "ID-123";
            });

    IdgenIdentifierController controller = new IdgenIdentifierController(service);

    Map<String, Object> response =
        controller
            .generateIdentifier(
                "42", new LinkedHashMap<>(Map.of("comment", "from-body")), "from-query")
            .getBody();

    assertEquals("ID-123", response.get("identifier"));
    assertEquals(serviceSource, receivedSource.get());
    assertEquals("from-query", receivedComment.get());
  }

  @Test
  void generateIdentifierFallsBackToBodyComment() {
    IdentifierSource serviceSource = new SequentialIdentifierGenerator();
    serviceSource.setUuid("uuid-456");

    AtomicReference<String> receivedComment = new AtomicReference<>();
    IdentifierSourceService service =
        createServiceStub(
            null,
            null,
            "uuid-456",
            serviceSource,
            (source, comment) -> {
              receivedComment.set(comment);
              return "ID-456";
            });

    IdgenIdentifierController controller = new IdgenIdentifierController(service);
    Map<String, Object> response =
        controller
            .generateIdentifier(
                "uuid-456", new LinkedHashMap<>(Map.of("comment", "from-body")), null)
            .getBody();

    assertEquals("ID-456", response.get("identifier"));
    assertEquals("from-body", receivedComment.get());
  }

  @Test
  void generateIdentifierFallsBackToUuidWhenNumericIdNotFound() {
    IdentifierSource serviceSource = new SequentialIdentifierGenerator();
    serviceSource.setUuid("42");

    AtomicReference<IdentifierSource> receivedSource = new AtomicReference<>();
    IdentifierSourceService service =
        createServiceStub(
            41,
            null,
            "42",
            serviceSource,
            (source, comment) -> {
              receivedSource.set(source);
              return "ID-FALLBACK";
            });

    IdgenIdentifierController controller = new IdgenIdentifierController(service);
    Map<String, Object> response =
        controller.generateIdentifier("42", new LinkedHashMap<>(), null).getBody();

    assertEquals("ID-FALLBACK", response.get("identifier"));
    assertEquals(serviceSource, receivedSource.get());
  }

  @Test
  void generateIdentifierFallsBackToUuidWhenNumericOverflowsInt() {
    IdentifierSource serviceSource = new SequentialIdentifierGenerator();
    serviceSource.setUuid("2147483648");

    AtomicReference<String> receivedComment = new AtomicReference<>();
    IdentifierSourceService service =
        createServiceStub(
            null,
            null,
            "2147483648",
            serviceSource,
            (source, comment) -> {
              receivedComment.set(comment);
              return "ID-OVERFLOW";
            });

    IdgenIdentifierController controller = new IdgenIdentifierController(service);
    Map<String, Object> response =
        controller.generateIdentifier("2147483648", null, "overflow").getBody();

    assertEquals("ID-OVERFLOW", response.get("identifier"));
    assertEquals("overflow", receivedComment.get());
  }

  @Test
  void generateIdentifierUsesEmptyCommentWhenNoCommentIsProvided() {
    IdentifierSource source = new SequentialIdentifierGenerator();
    source.setUuid("uuid-789");

    AtomicReference<String> receivedComment = new AtomicReference<>("not-empty");
    IdentifierSourceService service =
        createServiceStub(
            null,
            null,
            "uuid-789",
            source,
            (ignoredSource, comment) -> {
              receivedComment.set(comment);
              return "ID-789";
            });

    IdgenIdentifierController controller = new IdgenIdentifierController(service);
    Map<String, Object> response =
        controller.generateIdentifier("uuid-789", new LinkedHashMap<>(), null).getBody();

    assertEquals("ID-789", response.get("identifier"));
    assertEquals("", receivedComment.get());
  }

  @Test
  void generateIdentifierReturnsNullWhenBodyCommentIsNull() {
    IdentifierSource source = new SequentialIdentifierGenerator();
    source.setUuid("uuid-null");

    AtomicReference<String> receivedComment = new AtomicReference<>("not-empty");
    IdentifierSourceService service =
        createServiceStub(
            null,
            null,
            "uuid-null",
            source,
            (ignoredSource, comment) -> {
              receivedComment.set(comment);
              return "ID-NULL";
            });

    IdgenIdentifierController controller = new IdgenIdentifierController(service);
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("comment", null);
    Map<String, Object> response = controller.generateIdentifier("uuid-null", body, null).getBody();

    assertEquals("ID-NULL", response.get("identifier"));
    assertNull(receivedComment.get());
  }

  @Test
  void generateIdentifierFallsBackToUuidWhenSourceHasLeadingZerosAndNoNumericMatch() {
    IdentifierSource source = new SequentialIdentifierGenerator();
    source.setUuid("0007");

    AtomicReference<String> receivedComment = new AtomicReference<>();
    IdentifierSourceService service =
        createServiceStub(
            7,
            null,
            "0007",
            source,
            (ignoredSource, comment) -> {
              receivedComment.set(comment);
              return "ID-LEADING-ZERO";
            });

    IdgenIdentifierController controller = new IdgenIdentifierController(service);
    Map<String, Object> response =
        controller.generateIdentifier("0007", new LinkedHashMap<>(), null).getBody();

    assertEquals("ID-LEADING-ZERO", response.get("identifier"));
    assertEquals("0007", ((SequentialIdentifierGenerator) source).getUuid());
  }

  @Test
  void generateIdentifierReturns404WhenSourceUniqueIdIsBlank() {
    IdentifierSourceService service =
        createServiceStub(null, null, null, null, (source, comment) -> "should-not-happen");

    IdgenIdentifierController controller = new IdgenIdentifierController(service);
    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> controller.generateIdentifier("   ", new LinkedHashMap<>(), null));
    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
  }

  @Test
  void generateIdentifierReturns404WhenSourceUniqueIdIsNegativeLikeInt() {
    IdentifierSourceService service =
        createServiceStub(null, null, null, null, (source, comment) -> "should-not-happen");

    IdgenIdentifierController controller = new IdgenIdentifierController(service);
    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> controller.generateIdentifier("-42", new LinkedHashMap<>(), null));
    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
  }

  @Test
  void generateIdentifierFallsBackToUuidWhenSourceUniqueIdContainsWhitespaceOnlyAroundNumber() {
    IdentifierSource source = new SequentialIdentifierGenerator();
    source.setUuid(" 42 ");

    AtomicReference<IdentifierSource> receivedSource = new AtomicReference<>();
    IdentifierSourceService service =
        createServiceStub(
            42,
            null,
            " 42 ",
            source,
            (sourceById, comment) -> {
              receivedSource.set(sourceById);
              return "ID-WHITESPACE";
            });

    IdgenIdentifierController controller = new IdgenIdentifierController(service);
    Map<String, Object> response =
        controller.generateIdentifier(" 42 ", new LinkedHashMap<>(), null).getBody();

    assertEquals("ID-WHITESPACE", response.get("identifier"));
    assertEquals(source, receivedSource.get());
  }

  @Test
  void generateIdentifierPrefersQueryOverNullBodyComment() {
    IdentifierSource source = new SequentialIdentifierGenerator();
    source.setUuid("uuid-null-body");

    AtomicReference<String> receivedComment = new AtomicReference<>("initial");
    IdentifierSourceService service =
        createServiceStub(
            null,
            null,
            "uuid-null-body",
            source,
            (ignoredSource, comment) -> {
              receivedComment.set(comment);
              return "ID-QUERY-NULL-BODY";
            });

    IdgenIdentifierController controller = new IdgenIdentifierController(service);
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("comment", null);
    Map<String, Object> response =
        controller.generateIdentifier("uuid-null-body", body, "query-comment").getBody();

    assertEquals("ID-QUERY-NULL-BODY", response.get("identifier"));
    assertEquals("query-comment", receivedComment.get());
  }

  @Test
  void generateIdentifierPropagatesRuntimeExceptions() {
    IdentifierSource source = new SequentialIdentifierGenerator();
    source.setUuid("uuid-bad-service");

    IdentifierSourceService service =
        createServiceStub(
            null,
            null,
            "uuid-bad-service",
            source,
            (ignoredSource, ignoredComment) -> {
              throw new RuntimeException("boom");
            });

    IdgenIdentifierController controller = new IdgenIdentifierController(service);
    assertThrows(
        RuntimeException.class,
        () -> controller.generateIdentifier("uuid-bad-service", null, null));
  }

  @Test
  void generateIdentifierPropagatesApiException() {
    IdentifierSource source = new SequentialIdentifierGenerator();
    source.setUuid("uuid-api-error");

    IdentifierSourceService service =
        createServiceStub(
            null,
            null,
            "uuid-api-error",
            source,
            (ignoredSource, ignoredComment) -> {
              throw new APIException("api broken");
            });

    IdgenIdentifierController controller = new IdgenIdentifierController(service);
    assertThrows(
        APIException.class, () -> controller.generateIdentifier("uuid-api-error", null, null));
  }

  @Test
  void generateIdentifierKeepsEmptyQueryCommentOverBodyWhenBodyHasComment() {
    IdentifierSource source = new SequentialIdentifierGenerator();
    source.setUuid("uuid-empty-query");

    AtomicReference<String> receivedComment = new AtomicReference<>("not-empty");
    IdentifierSourceService service =
        createServiceStub(
            null,
            null,
            "uuid-empty-query",
            source,
            (ignoredSource, comment) -> {
              receivedComment.set(comment);
              return "ID-EMPTY-QUERY";
            });

    IdgenIdentifierController controller = new IdgenIdentifierController(service);
    Map<String, Object> response =
        controller
            .generateIdentifier(
                "uuid-empty-query", new LinkedHashMap<>(Map.of("comment", "from-body")), "")
            .getBody();

    assertEquals("ID-EMPTY-QUERY", response.get("identifier"));
    assertEquals("", receivedComment.get());
  }

  @Test
  void generateIdentifierIgnoresUnrelatedBodyProperties() {
    IdentifierSource source = new SequentialIdentifierGenerator();
    source.setUuid("uuid-unrelated");

    AtomicReference<String> receivedComment = new AtomicReference<>();
    IdentifierSourceService service =
        createServiceStub(
            null,
            null,
            "uuid-unrelated",
            source,
            (ignoredSource, comment) -> {
              receivedComment.set(comment);
              return "ID-UNRELATED";
            });

    IdgenIdentifierController controller = new IdgenIdentifierController(service);
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("comment", "from-body");
    body.put("other", "value");
    Map<String, Object> response =
        controller.generateIdentifier("uuid-unrelated", body, null).getBody();

    assertEquals("ID-UNRELATED", response.get("identifier"));
    assertEquals("from-body", receivedComment.get());
  }

  @Test
  void generateIdentifierAcceptsNonStringBodyCommentValues() {
    IdentifierSource source = new SequentialIdentifierGenerator();
    source.setUuid("uuid-number");

    AtomicReference<String> receivedComment = new AtomicReference<>();
    IdentifierSourceService service =
        createServiceStub(
            null,
            null,
            "uuid-number",
            source,
            (ignoredSource, comment) -> {
              receivedComment.set(comment);
              return "ID-NUM";
            });

    IdgenIdentifierController controller = new IdgenIdentifierController(service);
    Map<String, Object> response =
        controller
            .generateIdentifier("uuid-number", new LinkedHashMap<>(Map.of("comment", 12345)), null)
            .getBody();

    assertEquals("ID-NUM", response.get("identifier"));
    assertEquals("12345", receivedComment.get());
  }

  @Test
  void generateIdentifierReturnsCreatedStatusAndResponseBody() {
    IdentifierSource source = new SequentialIdentifierGenerator();
    source.setUuid("uuid-created");

    IdgenIdentifierController controller =
        new IdgenIdentifierController(
            createServiceStub(
                null,
                null,
                "uuid-created",
                source,
                (ignoredSource, ignoredComment) -> "ID-CREATED"));

    var response =
        controller.generateIdentifier("uuid-created", new LinkedHashMap<>(), "created-comment");

    assertNotNull(response);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals(Map.of("identifier", "ID-CREATED"), response.getBody());
  }

  @Test
  void generateIdentifierReturns404WhenSourceNotFound() {
    IdentifierSourceService service =
        createServiceStub(null, null, null, null, (source, comment) -> "should-not-happen");

    IdgenIdentifierController controller = new IdgenIdentifierController(service);
    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> controller.generateIdentifier("missing", null, null));
    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    assertInstanceOf(ResponseStatusException.class, ex);
  }

  @FunctionalInterface
  private interface GenerateAction {
    String generate(IdentifierSource source, String comment);
  }

  private IdentifierSourceService createServiceStub(
      Integer expectedNumericId,
      IdentifierSource sourceById,
      String expectedUuid,
      IdentifierSource sourceByUuid,
      GenerateAction generateAction) {
    return (IdentifierSourceService)
        Proxy.newProxyInstance(
            IdgenIdentifierControllerTest.class.getClassLoader(),
            new Class<?>[] {IdentifierSourceService.class},
            (proxy, method, args) -> {
              return switch (method.getName()) {
                case "onStartup" -> null;
                case "onShutdown" -> null;
                case "getIdentifierSource" -> {
                  Integer id = (Integer) args[0];
                  yield Objects.equals(id, expectedNumericId) ? sourceById : null;
                }
                case "getIdentifierSourceByUuid" -> {
                  String uuid = (String) args[0];
                  yield Objects.equals(uuid, expectedUuid) ? sourceByUuid : null;
                }
                case "generateIdentifier" -> {
                  IdentifierSource source = (IdentifierSource) args[0];
                  String comment = (String) args[1];
                  yield generateAction.generate(source, comment);
                }
                default -> throw new UnsupportedOperationException("Unhandled method: " + method);
              };
            });
  }
}
