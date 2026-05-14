package org.sihsalus.webservices.rest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class RestStaticModuleBoundaryTest {

    @Test
    void importedRestCommonSourcesDoNotUseOmodRuntimeLoading() throws IOException {
        Path sourceRoot = Path.of("src/main/java/org/openmrs/module/webservices");
        assertTrue(Files.isDirectory(sourceRoot), "REST upstream sources must remain local");

        List<Path> javaSources;
        try (Stream<Path> stream = Files.walk(sourceRoot)) {
            javaSources = stream.filter(path -> path.toString().endsWith(".java")).toList();
        }

        assertFalse(javaSources.isEmpty(), "REST upstream source import should not be empty");
        for (Path source : javaSources) {
            String content = Files.readString(source, StandardCharsets.UTF_8);
            assertFalse(content.contains("ModuleFactory"), source + " must not use dynamic module loading");
            assertFalse(content.contains("BaseModuleActivator"), source + " must not declare an OMOD activator");
            assertFalse(content.contains("ModuleActivator"), source + " must not declare an OMOD activator");
        }
    }
}
