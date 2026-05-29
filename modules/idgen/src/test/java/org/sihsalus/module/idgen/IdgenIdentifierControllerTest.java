package org.sihsalus.module.idgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.openmrs.module.idgen.IdentifierSource;
import org.openmrs.module.idgen.SequentialIdentifierGenerator;
import org.openmrs.module.idgen.service.IdentifierSourceService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;

class IdgenIdentifierControllerTest {

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
