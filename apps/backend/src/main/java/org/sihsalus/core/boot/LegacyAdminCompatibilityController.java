package org.sihsalus.core.boot;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LegacyAdminCompatibilityController {

  @GetMapping({"/admin", "/admin/", "/admin/index.htm"})
  ResponseEntity<Void> legacyAdminIndex(HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.FOUND)
        .location(URI.create(request.getContextPath() + "/api/admin/static-modules"))
        .build();
  }
}
