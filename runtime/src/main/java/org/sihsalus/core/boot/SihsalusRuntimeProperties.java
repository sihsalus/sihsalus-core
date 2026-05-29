package org.sihsalus.core.boot;

import java.util.Locale;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("sihsalus")
public class SihsalusRuntimeProperties {

  private final Admin admin = new Admin();

  private final Auth auth = new Auth();

  private final Openmrs openmrs = new Openmrs();

  private final Initializer initializer = new Initializer();

  private final Ocl ocl = new Ocl();

  private final Scheduler scheduler = new Scheduler();

  public Admin getAdmin() {
    return admin;
  }

  public Auth getAuth() {
    return auth;
  }

  public Openmrs getOpenmrs() {
    return openmrs;
  }

  public Initializer getInitializer() {
    return initializer;
  }

  public Ocl getOcl() {
    return ocl;
  }

  public Scheduler getScheduler() {
    return scheduler;
  }

  public enum AuthMode {
    FRONTEND,
    KEYCLOAK;

    public static AuthMode from(String mode) {
      String normalized = mode.trim().toLowerCase(Locale.ROOT);
      return switch (normalized) {
        case "frontend", "local", "openmrs", "basic" -> FRONTEND;
        case "keycloak", "oauth2" -> KEYCLOAK;
        default ->
            throw new IllegalStateException(
                "Unsupported SIHSALUS_AUTH_MODE: " + mode + " (expected frontend or keycloak)");
      };
    }
  }

  public static final class Admin {

    private String username = "admin";

    private String password = "";

    public String getUsername() {
      return username;
    }

    public void setUsername(String username) {
      this.username = username;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(String password) {
      this.password = password;
    }
  }

  public static final class Auth {

    private String mode = "";

    private boolean oauth2Enabled;

    public String getMode() {
      return mode;
    }

    public void setMode(String mode) {
      this.mode = mode;
    }

    public boolean isOauth2Enabled() {
      return oauth2Enabled;
    }

    public void setOauth2Enabled(boolean oauth2Enabled) {
      this.oauth2Enabled = oauth2Enabled;
    }
  }

  public static final class Openmrs {

    private String applicationDataDirectory = "";

    public String getApplicationDataDirectory() {
      return applicationDataDirectory;
    }

    public void setApplicationDataDirectory(String applicationDataDirectory) {
      this.applicationDataDirectory = applicationDataDirectory;
    }
  }

  public static final class Initializer {

    private String startupLoad = "";

    private String domains = "";

    private final Exclude exclude = new Exclude();

    public String getStartupLoad() {
      return startupLoad;
    }

    public void setStartupLoad(String startupLoad) {
      this.startupLoad = startupLoad;
    }

    public String getDomains() {
      return domains;
    }

    public void setDomains(String domains) {
      this.domains = domains;
    }

    public Exclude getExclude() {
      return exclude;
    }

    public static final class Exclude {

      private String addresshierarchy = "";

      public String getAddresshierarchy() {
        return addresshierarchy;
      }

      public void setAddresshierarchy(String addresshierarchy) {
        this.addresshierarchy = addresshierarchy;
      }
    }
  }

  public static final class Ocl {

    private final StaticImport staticImport = new StaticImport();

    public StaticImport getStaticImport() {
      return staticImport;
    }

    public static final class StaticImport {

      private boolean enabled = true;

      private boolean failOnErrors = true;

      public boolean isEnabled() {
        return enabled;
      }

      public void setEnabled(boolean enabled) {
        this.enabled = enabled;
      }

      public boolean isFailOnErrors() {
        return failOnErrors;
      }

      public void setFailOnErrors(boolean failOnErrors) {
        this.failOnErrors = failOnErrors;
      }
    }
  }

  public static final class Scheduler {

    private final Startup startup = new Startup();

    public Startup getStartup() {
      return startup;
    }

    public static final class Startup {

      private boolean enabled = true;

      public boolean isEnabled() {
        return enabled;
      }

      public void setEnabled(boolean enabled) {
        this.enabled = enabled;
      }
    }
  }
}
