package org.sihsalus.core.boot;

import java.util.List;
import org.sihsalus.core.api.SihsalusModuleDescriptor;
import org.sihsalus.core.api.StaticModuleCatalog;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StaticModuleController {

  @GetMapping("/api/admin/static-modules")
  List<SihsalusModuleDescriptor> modules() {
    return StaticModuleCatalog.staticInternalModules();
  }
}
