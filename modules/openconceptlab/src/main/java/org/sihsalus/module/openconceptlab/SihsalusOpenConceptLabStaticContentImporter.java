package org.sihsalus.module.openconceptlab;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipFile;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.GlobalProperty;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.openconceptlab.Import;
import org.openmrs.module.openconceptlab.ImportService;
import org.openmrs.module.openconceptlab.ItemState;
import org.openmrs.module.openconceptlab.OpenConceptLabConstants;
import org.openmrs.module.openconceptlab.importer.Importer;
import org.openmrs.util.PrivilegeConstants;
import org.sihsalus.initializer.SihsalusContentPaths;
import org.sihsalus.initializer.StaticSihsalusContentLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

final class SihsalusOpenConceptLabStaticContentImporter
    implements ApplicationListener<ContextRefreshedEvent> {

  private static final Logger log =
      LoggerFactory.getLogger(SihsalusOpenConceptLabStaticContentImporter.class);

  private static final String OCL_DOMAIN = "ocl";

  private static final String STATIC_IMPORT_MARKER_PREFIX = "sihsalus.ocl.staticImport.sha256.";

  private static final String[] STARTUP_PRIVILEGES = {
    PrivilegeConstants.GET_GLOBAL_PROPERTIES,
    PrivilegeConstants.MANAGE_GLOBAL_PROPERTIES,
    PrivilegeConstants.MANAGE_CONCEPTS
  };

  private final AtomicBoolean initialized = new AtomicBoolean();

  private final Importer importer;

  private final ImportService importService;

  private final AdministrationService administrationService;

  private final StaticSihsalusContentLoader contentLoader;

  private final boolean enabled;

  private final boolean failOnErrors;

  SihsalusOpenConceptLabStaticContentImporter(
      Importer importer,
      ImportService importService,
      AdministrationService administrationService,
      StaticSihsalusContentLoader contentLoader,
      boolean enabled,
      boolean failOnErrors) {
    this.importer = importer;
    this.importService = importService;
    this.administrationService = administrationService;
    this.contentLoader = contentLoader;
    this.enabled = enabled;
    this.failOnErrors = failOnErrors;
  }

  @Override
  public void onApplicationEvent(ContextRefreshedEvent event) {
    if (!initialized.compareAndSet(false, true)) {
      return;
    }
    if (!enabled) {
      log.info("Static Open Concept Lab content import is disabled");
      return;
    }

    importStaticContent();
  }

  private void importStaticContent() {
    Path oclDirectory = resolveOclDirectory();

    markInProgressImportsAsFailed();
    ensureLoadAtStartupPath(oclDirectory);

    for (Path zipPath : listZipFiles(oclDirectory)) {
      String checksum = checksum(zipPath);
      if (hasImportMarker(checksum)) {
        log.info("Skipping previously imported Open Concept Lab package {}", zipPath.getFileName());
        continue;
      }

      importZip(zipPath);
      Import lastImport = getLastImport();
      int ignoredErrors = handleImportResult(zipPath, lastImport);
      saveImportMarker(checksum, zipPath, lastImport, ignoredErrors);
    }

    contentLoader.loadPostConceptDomains();
  }

  private Path resolveOclDirectory() {
    try {
      Path configRoot = SihsalusContentPaths.resolveConfigRoot();
      if (configRoot == null) {
        throw new IllegalStateException(
            "Static Open Concept Lab import is enabled but the SIH Salus content "
                + "configuration root is not available. Set SIHSALUS_INITIALIZER_SOURCE_ROOT "
                + "or disable sihsalus.ocl.static-import.enabled for infrastructure smoke tests.");
      }
      Path oclDirectory = SihsalusContentPaths.resolveDomainDirectory(configRoot, OCL_DOMAIN);
      if (oclDirectory == null) {
        throw new IllegalStateException(
            "Static Open Concept Lab import is enabled but the ocl content directory is not "
                + "available under "
                + configRoot);
      }
      return oclDirectory;
    } catch (IOException e) {
      throw new IllegalStateException(
          "Failed to resolve static Open Concept Lab content directory", e);
    }
  }

  private List<Path> listZipFiles(Path oclDirectory) {
    List<Path> zipFiles = new ArrayList<>();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(oclDirectory)) {
      for (Path path : stream) {
        if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
            && path.getFileName().toString().endsWith(".zip")) {
          zipFiles.add(path);
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to list static Open Concept Lab content packages", e);
    }

    zipFiles.sort(Comparator.comparing(path -> path.getFileName().toString()));
    return zipFiles;
  }

  private void importZip(Path zipPath) {
    log.info("Importing static Open Concept Lab package {}", zipPath.getFileName());
    try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
      importer.run(zipFile);
    } catch (IOException e) {
      throw new IllegalStateException(
          "Failed to open static Open Concept Lab package " + zipPath.getFileName(), e);
    }
  }

  private void markInProgressImportsAsFailed() {
    withStartupPrivileges(
        () -> {
          List<Import> inProgressImports = importService.getInProgressImports();
          for (Import inProgressImport : inProgressImports) {
            importService.failImport(inProgressImport, "System interruption during import");
            importService.stopImport(inProgressImport);
          }
        });
  }

  private void ensureLoadAtStartupPath(Path oclDirectory) {
    withStartupPrivileges(
        () -> {
          String loadAtStartupPath =
              administrationService.getGlobalProperty(
                  OpenConceptLabConstants.GP_OCL_LOAD_AT_STARTUP_PATH);
          if (StringUtils.isBlank(loadAtStartupPath)) {
            administrationService.saveGlobalProperty(
                new GlobalProperty(
                    OpenConceptLabConstants.GP_OCL_LOAD_AT_STARTUP_PATH,
                    oclDirectory.toString(),
                    "Static Open Concept Lab content package directory"));
          }
        });
  }

  private boolean hasImportMarker(String checksum) {
    return withStartupPrivileges(
        () -> administrationService.getGlobalPropertyObject(markerPropertyName(checksum)) != null);
  }

  private Import getLastImport() {
    return withStartupPrivileges(importService::getLastImport);
  }

  private int handleImportResult(Path zipPath, Import lastImport) {
    if (lastImport == null) {
      throw new IllegalStateException(
          "Static Open Concept Lab import failed for "
              + zipPath.getFileName()
              + ": No import record was created");
    }

    Integer errorCount = getErrorItemCount(lastImport);
    if (errorCount != null && errorCount > 0) {
      if (failOnErrors) {
        throw new IllegalStateException(
            "Static Open Concept Lab import failed for "
                + zipPath.getFileName()
                + ": "
                + lastImport.getErrorMessage());
      }
      ignoreImportErrors(lastImport);
      log.warn(
          "Static Open Concept Lab package {} completed with {} item errors; marked them as"
              + " ignored",
          zipPath.getFileName(),
          errorCount);
      return errorCount;
    }

    if (StringUtils.isNotBlank(lastImport.getErrorMessage())) {
      throw new IllegalStateException(
          "Static Open Concept Lab import failed for "
              + zipPath.getFileName()
              + ": "
              + lastImport.getErrorMessage());
    }
    return 0;
  }

  private Integer getErrorItemCount(Import oclImport) {
    return withStartupPrivileges(
        () -> importService.getImportItemsCount(oclImport, Collections.singleton(ItemState.ERROR)));
  }

  private void ignoreImportErrors(Import oclImport) {
    withStartupPrivileges(() -> importService.ignoreAllErrors(oclImport));
  }

  private void saveImportMarker(
      String checksum, Path zipPath, Import oclImport, int ignoredErrors) {
    withStartupPrivileges(
        () -> {
          String propertyName = markerPropertyName(checksum);
          GlobalProperty marker = administrationService.getGlobalPropertyObject(propertyName);
          if (marker == null) {
            marker = new GlobalProperty(propertyName);
            marker.setDescription("Static Open Concept Lab content package checksum");
          }
          marker.setPropertyValue(
              zipPath.getFileName()
                  + ":"
                  + oclImport.getUuid()
                  + ":ignoredErrors="
                  + ignoredErrors);
          administrationService.saveGlobalProperty(marker);
        });
  }

  private String checksum(Path path) {
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 digest is not available", e);
    }

    try (InputStream input = Files.newInputStream(path);
        DigestInputStream digestInput = new DigestInputStream(input, digest)) {
      digestInput.transferTo(OutputStream.nullOutputStream());
      return HexFormat.of().formatHex(digest.digest());
    } catch (IOException e) {
      throw new IllegalStateException(
          "Failed to checksum static Open Concept Lab package " + path.getFileName(), e);
    }
  }

  private String markerPropertyName(String checksum) {
    return STATIC_IMPORT_MARKER_PREFIX + checksum;
  }

  private static void withStartupPrivileges(Runnable task) {
    withStartupPrivileges(
        () -> {
          task.run();
          return null;
        });
  }

  private static <T> T withStartupPrivileges(PrivilegedSupplier<T> task) {
    boolean openedSession = !Context.isSessionOpen();
    if (openedSession) {
      Context.openSession();
    }

    Context.addProxyPrivilege(STARTUP_PRIVILEGES);
    try {
      return task.get();
    } finally {
      Context.removeProxyPrivilege(STARTUP_PRIVILEGES);
      if (openedSession) {
        Context.closeSession();
      }
    }
  }

  @FunctionalInterface
  private interface PrivilegedSupplier<T> {
    T get();
  }
}
