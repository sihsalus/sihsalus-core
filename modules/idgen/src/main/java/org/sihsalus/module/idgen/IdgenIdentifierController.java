package org.sihsalus.module.idgen;

import java.util.LinkedHashMap;
import java.util.Map;
import org.openmrs.module.idgen.IdentifierSource;
import org.openmrs.module.idgen.service.IdentifierSourceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class IdgenIdentifierController {

  private final IdentifierSourceService identifierSourceService;

  public IdgenIdentifierController(IdentifierSourceService identifierSourceService) {
    this.identifierSourceService = identifierSourceService;
  }

  /**
   * Exposes the legacy and REST module identifier-generation endpoint variations used by UI and
   * integrations.
   */
  @PostMapping({
    "/rest/v1/idgen/identifiersource/{sourceUniqueId}/identifier",
    "/ws/rest/v1/idgen/identifiersource/{sourceUniqueId}/identifier",
    "/rest/v1/idgen/{sourceUniqueId}/identifier",
    "/ws/rest/v1/idgen/{sourceUniqueId}/identifier",
    "/ws/rest/v1/idgen/identifierSource/{sourceUniqueId}/identifier"
  })
  ResponseEntity<Map<String, Object>> generateIdentifier(
      @PathVariable("sourceUniqueId") String sourceUniqueId,
      @RequestBody(required = false) Map<String, Object> body,
      @RequestParam(value = "comment", required = false) String commentParam) {
    IdentifierSource source = getIdentifierSource(sourceUniqueId);
    String identifier =
        identifierSourceService.generateIdentifier(source, comment(body, commentParam));

    Map<String, Object> response = new LinkedHashMap<>();
    response.put("identifier", identifier);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  private IdentifierSource getIdentifierSource(String sourceUniqueId) {
    IdentifierSource source = null;
    if (isInteger(sourceUniqueId)) {
      try {
        source = identifierSourceService.getIdentifierSource(Integer.valueOf(sourceUniqueId));
      } catch (NumberFormatException ignored) {
        // Fall through to UUID lookup.
      }
    }
    if (source == null) {
      source = identifierSourceService.getIdentifierSourceByUuid(sourceUniqueId);
    }
    if (source == null) {
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND, "Identifier source not found: " + sourceUniqueId);
    }
    return source;
  }

  private boolean isInteger(String value) {
    return value != null && !value.isBlank() && value.chars().allMatch(Character::isDigit);
  }

  private String comment(Map<String, Object> body, String commentParam) {
    if (commentParam != null) {
      return commentParam;
    }
    if (body != null && body.containsKey("comment")) {
      Object comment = body.get("comment");
      return comment == null ? null : comment.toString();
    }
    return "";
  }
}
