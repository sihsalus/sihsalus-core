package org.sihsalus.core.boot;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.sihsalus.core.api.SihsalusModuleDescriptor;
import org.sihsalus.initializer.SihsalusContentPaths;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
final class StaticModuleRuntimeInspector {

  private final ApplicationContext applicationContext;

  private final JdbcTemplate jdbcTemplate;

  private final Environment environment;

  private final SihsalusRuntimeProperties runtimeProperties;

  StaticModuleRuntimeInspector(
      ApplicationContext applicationContext,
      JdbcTemplate jdbcTemplate,
      Environment environment,
      SihsalusRuntimeProperties runtimeProperties) {
    this.applicationContext = applicationContext;
    this.jdbcTemplate = jdbcTemplate;
    this.environment = environment;
    this.runtimeProperties = runtimeProperties;
  }

  StaticModuleRuntimeState inspect(SihsalusModuleDescriptor module) {
    String moduleId = module.id();
    boolean configured =
        environment.getProperty("sihsalus.modules." + moduleId + ".enabled", Boolean.class, true);
    boolean springRegistered = springRegistered(module);
    boolean databaseManaged = module.isDatabaseManaged();
    boolean databaseMigrated = databaseManaged && databaseMigrated(module);
    int activeScheduledTasks = activeScheduledTasks(module);

    Map<String, Object> details = new LinkedHashMap<>();
    details.put("lifecycle", lifecycle(configured, springRegistered));
    details.put("configurationBean", module.configBean() == null ? "" : module.configBean());
    details.put("legacySchedulerControlled", module.hasLegacyScheduler());
    addContentDetails(moduleId, details);

    return new StaticModuleRuntimeState(
        true,
        configured,
        springRegistered,
        configured && springRegistered,
        databaseManaged,
        databaseMigrated,
        activeScheduledTasks,
        details);
  }

  private boolean springRegistered(SihsalusModuleDescriptor module) {
    String beanName = module.configBean();
    return beanName != null && applicationContext.containsBean(beanName);
  }

  private boolean databaseMigrated(SihsalusModuleDescriptor module) {
    try {
      Integer count =
          jdbcTemplate.queryForObject(
              "select count(*) from liquibasechangelog where filename = ?",
              Integer.class,
              module.liquibaseFile());
      return count != null && count > 0;
    } catch (DataAccessException e) {
      return false;
    }
  }

  private int activeScheduledTasks(SihsalusModuleDescriptor module) {
    String classPrefix = module.schedulerPrefix();
    if (classPrefix == null) {
      return 0;
    }
    try {
      Integer count =
          jdbcTemplate.queryForObject(
              "select count(*) from scheduler_task_config where schedulable_class like ? and"
                  + " (started = true or start_on_startup = true)",
              Integer.class,
              classPrefix + "%");
      return count == null ? 0 : count;
    } catch (DataAccessException e) {
      return 0;
    }
  }

  private String lifecycle(boolean configured, boolean springRegistered) {
    if (!configured) {
      return "disabled";
    }
    if (!springRegistered) {
      return "compiled_only";
    }
    return "started";
  }

  private void addContentDetails(String moduleId, Map<String, Object> details) {
    if ("initializer".equals(moduleId)) {
      details.put(
          "startupLoad",
          defaultIfBlank(runtimeProperties.getInitializer().getStartupLoad(), "continue_on_error"));
      details.put("sourceRoot", SihsalusContentPaths.resolveSourceRoot().toString());
      details.put("configurationRootAvailable", configurationRootAvailable());
    }
    if ("openconceptlab".equals(moduleId)) {
      details.put("staticImportEnabled", runtimeProperties.getOcl().getStaticImport().isEnabled());
      details.put(
          "staticImportFailOnErrors",
          runtimeProperties.getOcl().getStaticImport().isFailOnErrors());
      details.put("oclContentDirectoryAvailable", oclContentDirectoryAvailable());
    }
  }

  private String defaultIfBlank(String value, String defaultValue) {
    return value == null || value.isBlank() ? defaultValue : value;
  }

  private boolean configurationRootAvailable() {
    try {
      return SihsalusContentPaths.resolveConfigRoot() != null;
    } catch (Exception e) {
      return false;
    }
  }

  private boolean oclContentDirectoryAvailable() {
    try {
      Path configRoot = SihsalusContentPaths.resolveConfigRoot();
      return configRoot != null && Files.isDirectory(configRoot.resolve("ocl"));
    } catch (Exception e) {
      return false;
    }
  }
}
