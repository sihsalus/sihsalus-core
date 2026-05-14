package org.sihsalus.webservices.rest;

import java.util.Map;
import org.sihsalus.core.api.StaticModuleCatalog;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SystemStatusController {

    @GetMapping("/api/system/info")
    Map<String, Object> info() {
        return Map.of(
                "name", "SIH Salus Core",
                "moduleModel", "static-maven-reactor",
                "dynamicOmodLoading", false,
                "internalModules", StaticModuleCatalog.modules().size());
    }
}
