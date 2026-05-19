package org.openmrs.module.initializer;

public final class InitializerConstants {

  public static final String MODULE_ARTIFACT_ID = "initializer";

  public static final String ARG_DOMAINS = "domains";

  public static final String PROPS_DOMAINS = MODULE_ARTIFACT_ID + "." + ARG_DOMAINS;

  public static final String ARG_EXCLUDE = "exclude";

  public static final String PROPS_EXCLUDE = MODULE_ARTIFACT_ID + "." + ARG_EXCLUDE;

  public static final String PROPS_STARTUP_LOAD = MODULE_ARTIFACT_ID + ".startup.load";

  public static final String PROPS_STARTUP_LOAD_CONTINUE_ON_ERROR = "continue_on_error";

  public static final String PROPS_STARTUP_LOAD_FAIL_ON_ERROR = "fail_on_error";

  public static final String PROPS_STARTUP_LOAD_DISABLED = "disabled";

  private InitializerConstants() {}
}
