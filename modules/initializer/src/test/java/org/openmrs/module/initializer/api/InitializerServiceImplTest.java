package org.openmrs.module.initializer.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.openmrs.module.initializer.InitializerConstants.PROPS_STARTUP_LOAD;
import static org.openmrs.module.initializer.InitializerConstants.PROPS_STARTUP_LOAD_DISABLED;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openmrs.api.context.Context;

class InitializerServiceImplTest {

  @TempDir private Path tempDir;

  private Properties previousRuntimeProperties;

  private String previousSourceRoot;

  @BeforeEach
  void captureEnvironment() {
    previousRuntimeProperties = Context.getRuntimeProperties();
    previousSourceRoot = System.getProperty("sihsalus.initializer.sourceRoot");
    System.setProperty("sihsalus.initializer.sourceRoot", tempDir.toString());
  }

  @AfterEach
  void restoreEnvironment() {
    Context.setRuntimeProperties(previousRuntimeProperties);
    if (previousSourceRoot == null) {
      System.clearProperty("sihsalus.initializer.sourceRoot");
    } else {
      System.setProperty("sihsalus.initializer.sourceRoot", previousSourceRoot);
    }
  }

  @Test
  void runtimePropertiesOverrideJsonKeyValues() throws Exception {
    writeJsonKeyValues(
        """
        {
          "initializer.startup.load": "fail_on_error",
          "custom.key": "json-value"
        }
        """);

    Properties runtimeProperties = new Properties();
    runtimeProperties.setProperty(PROPS_STARTUP_LOAD, PROPS_STARTUP_LOAD_DISABLED);
    Context.setRuntimeProperties(runtimeProperties);

    InitializerServiceImpl service = new InitializerServiceImpl(List.of());

    assertEquals(PROPS_STARTUP_LOAD_DISABLED, service.getValueFromKey(PROPS_STARTUP_LOAD));
    assertEquals("json-value", service.getValueFromKey("custom.key"));
  }

  @Test
  void disabledStartupModeDoesNotParseBrokenJsonKeyValues() throws Exception {
    writeJsonKeyValues("{");

    Properties runtimeProperties = new Properties();
    runtimeProperties.setProperty(PROPS_STARTUP_LOAD, PROPS_STARTUP_LOAD_DISABLED);
    Context.setRuntimeProperties(runtimeProperties);

    InitializerServiceImpl service = new InitializerServiceImpl(List.of());

    assertEquals(
        PROPS_STARTUP_LOAD_DISABLED, service.getInitializerConfig().getStartupLoadingMode());
  }

  private void writeJsonKeyValues(String content) throws Exception {
    Path directory =
        tempDir.resolve("configuration").resolve("backend_configuration").resolve("jsonkeyvalues");
    Files.createDirectories(directory);
    Files.writeString(directory.resolve("values.json"), content);
  }
}
