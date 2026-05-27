package org.openmrs.module.oauth2login.authscheme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class UserInfoTest {

  @Test
  void serviceAccountCredentialsUseServiceAccountUsernameMapping() {
    UserInfo userInfo =
        userInfo(
            """
                {
                  "preferred_username": "alice",
                  "client_id": "backend-client",
                  "roles": ["Provider", "Nurse"]
                }
                """);

    assertEquals("alice", new OAuth2TokenCredentials(userInfo).getClientName());
    assertEquals("backend-client", new OAuth2TokenCredentials(userInfo, true).getClientName());
    assertEquals(List.of("Provider", "Nurse"), userInfo.getRoleNames());
  }

  @Test
  void serviceAccountUsernameFallsBackToRegularUsername() {
    Properties properties = defaultProperties();
    properties.remove(UserInfo.PROP_USERNAME_SERVICE_ACCOUNT);
    UserInfo userInfo =
        new UserInfo(properties, "{\"preferred_username\":\"alice\",\"roles\":[\"Provider\"]}");

    assertEquals("alice", new OAuth2TokenCredentials(userInfo, true).getClientName());
  }

  @Test
  void missingRolesMappingLeavesRolesUnmanaged() {
    Properties properties = defaultProperties();
    properties.remove(UserInfo.PROP_ROLES);
    UserInfo userInfo = new UserInfo(properties, "{\"preferred_username\":\"alice\"}");

    assertNull(userInfo.getRoleNames());
  }

  @Test
  void configuredButMissingRolesClaimFailsAuthenticationDataParsing() {
    UserInfo userInfo = userInfo("{\"preferred_username\":\"alice\"}");

    assertThrows(IllegalArgumentException.class, userInfo::getRoleNames);
  }

  @Test
  void rolesClaimMustBeAJsonArrayOfStrings() {
    UserInfo userInfo = userInfo("{\"preferred_username\":\"alice\",\"roles\":\"Provider\"}");

    assertThrows(IllegalArgumentException.class, userInfo::getRoleNames);
  }

  private UserInfo userInfo(String json) {
    return new UserInfo(defaultProperties(), json);
  }

  private Properties defaultProperties() {
    Properties properties = new Properties();
    properties.setProperty(UserInfo.PROP_USERNAME, "preferred_username");
    properties.setProperty(UserInfo.PROP_USERNAME_SERVICE_ACCOUNT, "client_id");
    properties.setProperty(UserInfo.PROP_ROLES, "roles");
    return properties;
  }
}
