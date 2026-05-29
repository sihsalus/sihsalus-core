package org.sihsalus.core.boot;

import java.util.LinkedHashMap;
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

  private final StaticModuleRuntimeInspector runtimeInspector;

  StaticModuleController(StaticModuleRuntimeInspector runtimeInspector) {
    this.runtimeInspector = runtimeInspector;
  }

  @GetMapping("/api/admin/static-modules")
  List<Map<String, Object>> modules() {
    return StaticModuleCatalog.staticInternalModules().stream().map(this::toAdminModule).toList();
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
    StaticModuleRuntimeState runtimeState = runtimeInspector.inspect(module);
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("uuid", moduleId);
    response.put("display", moduleId);
    response.put("name", moduleId);
    response.put("version", module.baselineVersion());
    response.put("started", runtimeState.started());
    response.put("compiled", runtimeState.compiled());
    response.put("configured", runtimeState.configured());
    response.put("springRegistered", runtimeState.springRegistered());
    response.put("databaseManaged", runtimeState.databaseManaged());
    response.put("databaseMigrated", runtimeState.databaseMigrated());
    response.put("activeScheduledTasks", runtimeState.activeScheduledTasks());
    response.put("runtime", runtimeState.details());
    return response;
  }

  private Map<String, Object> toAdminModule(SihsalusModuleDescriptor module) {
    StaticModuleRuntimeState runtimeState = runtimeInspector.inspect(module);
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("id", module.id());
    response.put("sourceModule", module.sourceModule());
    response.put("baselineVersion", module.baselineVersion());
    response.put("status", module.status());
    response.put("compiled", runtimeState.compiled());
    response.put("configured", runtimeState.configured());
    response.put("springRegistered", runtimeState.springRegistered());
    response.put("started", runtimeState.started());
    response.put("databaseManaged", runtimeState.databaseManaged());
    response.put("databaseMigrated", runtimeState.databaseMigrated());
    response.put("activeScheduledTasks", runtimeState.activeScheduledTasks());
    response.put("runtime", runtimeState.details());
    return response;
  }

  private String restModuleId(SihsalusModuleDescriptor module) {
    return stripSuffix(stripSuffix(module.sourceModule(), "-omod"), "-api");
  }

  private String stripSuffix(String value, String suffix) {
    return value.endsWith(suffix) ? value.substring(0, value.length() - suffix.length()) : value;
  }
}
