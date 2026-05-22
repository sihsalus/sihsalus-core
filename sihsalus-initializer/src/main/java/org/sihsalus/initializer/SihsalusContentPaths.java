package org.sihsalus.initializer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class SihsalusContentPaths {

  public static final String CONFIGURATION_ROOT = "configuration/backend_configuration";

  private SihsalusContentPaths() {}

  public static Path resolveConfigRoot() throws IOException {
    Path sourceRoot = resolveSourceRoot();
    if (!Files.isDirectory(sourceRoot)) {
      return null;
    }

    Path realSourceRoot = sourceRoot.toRealPath();
    Path configRoot = realSourceRoot.resolve(CONFIGURATION_ROOT).normalize();
    if (!Files.isDirectory(configRoot)) {
      return null;
    }

    Path realConfigRoot = configRoot.toRealPath();
    if (!realConfigRoot.startsWith(realSourceRoot)) {
      throw new IllegalStateException(
          "SIH Salus content configuration escapes the configured source directory.");
    }
    return realConfigRoot;
  }

  public static Path resolveSourceRoot() {
    Path sourceLayout = Paths.get(InitializerBoundary.sourceLayout());
    if (sourceLayout.isAbsolute()) {
      return sourceLayout.normalize();
    }

    Path current = Paths.get("").toAbsolutePath().normalize();
    for (Path candidate = current; candidate != null; candidate = candidate.getParent()) {
      Path resolved = candidate.resolve(sourceLayout).normalize();
      if (Files.isDirectory(resolved)) {
        return resolved;
      }
    }
    return current.resolve(sourceLayout).normalize();
  }

  public static Path resolveDomainDirectory(Path configRoot, String domain) throws IOException {
    Path directory = configRoot.resolve(domain).normalize();
    if (!directory.startsWith(configRoot) || !Files.isDirectory(directory)) {
      return null;
    }

    Path realDirectory = directory.toRealPath();
    if (!realDirectory.startsWith(configRoot)) {
      throw new IllegalStateException(
          "SIH Salus content domain " + domain + " escapes content configuration.");
    }
    return realDirectory;
  }
}
