package org.sihsalus.fhir2;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class Fhir2StaticModuleBoundaryTest {

    @Test
    void importedFhirSourcesDoNotUseOmodRuntimeLoading() throws IOException {
        Path sourceRoot = Path.of("src/main/java/org/openmrs/module/fhir2");
        assertTrue(Files.isDirectory(sourceRoot), "FHIR2 upstream sources must remain local");

        List<Path> javaSources;
        try (Stream<Path> stream = Files.walk(sourceRoot)) {
            javaSources = stream.filter(path -> path.toString().endsWith(".java")).toList();
        }

        assertFalse(javaSources.isEmpty(), "FHIR2 upstream source import should not be empty");
        for (Path source : javaSources) {
            String content = Files.readString(source, StandardCharsets.UTF_8);
            assertFalse(content.contains("ModuleFactory"), source + " must not use dynamic module loading");
            assertFalse(content.contains("BaseModuleActivator"), source + " must not declare an OMOD activator");
            assertFalse(content.contains("ModuleActivator"), source + " must not declare an OMOD activator");
        }
    }
}
