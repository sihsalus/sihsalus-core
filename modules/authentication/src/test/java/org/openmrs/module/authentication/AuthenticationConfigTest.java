package org.openmrs.module.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.openmrs.api.context.AuthenticationScheme;

class AuthenticationConfigTest {

  @AfterEach
  void resetConfig() {
    AuthenticationConfig.setConfig(new Properties());
  }

  @Test
  void missingExplicitSchemeFailsClearly() {
    AuthenticationConfig.setConfig(new Properties());

    assertThrows(
        IllegalStateException.class, () -> AuthenticationConfig.getAuthenticationScheme("missing"));
  }

  @Test
  void configuredClassMustImplementAuthenticationScheme() {
    Properties config = new Properties();
    config.setProperty(
        AuthenticationConfig.SCHEME_TYPE_TEMPLATE.replace(AuthenticationConfig.SCHEME_ID, "bad"),
        String.class.getName());
    AuthenticationConfig.setConfig(config);

    assertThrows(
        IllegalStateException.class,
        () ->
            AuthenticationConfig.getClass(
                AuthenticationConfig.SCHEME_TYPE_TEMPLATE.replace(
                    AuthenticationConfig.SCHEME_ID, "bad"),
                AuthenticationScheme.class));
  }

  @Test
  void stringListsAreTrimmedAndBlankEntriesAreIgnored() {
    assertEquals(
        List.of("/login.htm", "/ws/**/*"),
        AuthenticationUtil.getStringList(" /login.htm, , /ws/**/* ", ","));
  }
}
