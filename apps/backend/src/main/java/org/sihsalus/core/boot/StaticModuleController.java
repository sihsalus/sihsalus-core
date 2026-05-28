package org.sihsalus.core.boot;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.sihsalus.core.api.SihsalusModuleDescriptor;
import org.sihsalus.core.api.StaticModuleCatalog;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class StaticModuleController {

  @GetMapping("/api/admin/static-modules")
  List<SihsalusModuleDescriptor> modules() {
    return StaticModuleCatalog.staticInternalModules();
  }

  @GetMapping("/rest/v1/module")
  Map<String, Object> restModules() {
    List<Map<String, Object>> modules =
        StaticModuleCatalog.staticInternalModules().stream().map(this::toRestModule).toList();

    return Map.of("results", modules);
  }

  @GetMapping("/rest/v1/module/{moduleId}")
  Map<String, Object> restModule(@PathVariable("moduleId") String moduleId) {
    Optional<Map<String, Object>> module =
        StaticModuleCatalog.staticInternalModules().stream()
            .filter(descriptor -> restModuleId(descriptor).equals(moduleId))
            .map(this::toRestModule)
            .findFirst();

    return module.orElseThrow(
        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Module not found: " + moduleId));
  }

  private Map<String, Object> toRestModule(SihsalusModuleDescriptor module) {
    String moduleId = restModuleId(module);
    return Map.of(
        "uuid", moduleId,
        "display", moduleId,
        "name", moduleId,
        "version", module.baselineVersion(),
        "started", true);
  }

  private String restModuleId(SihsalusModuleDescriptor module) {
    return stripSuffix(stripSuffix(module.sourceModule(), "-omod"), "-api");
  }

  private String stripSuffix(String value, String suffix) {
    return value.endsWith(suffix) ? value.substring(0, value.length() - suffix.length()) : value;
  }
}
