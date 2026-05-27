package org.sihsalus.initializer;

public final class InitializerBoundary {

  private InitializerBoundary() {}

  public static String sourceLayout() {
    String systemProperty = System.getProperty("sihsalus.initializer.sourceRoot");
    if (systemProperty != null && !systemProperty.isBlank()) {
      return systemProperty;
    }
    String environmentValue = System.getenv("SIHSALUS_INITIALIZER_SOURCE_ROOT");
    if (environmentValue != null && !environmentValue.isBlank()) {
      return environmentValue;
    }
    return ".dev/reference-sources/sihsalus-content";
  }
}
