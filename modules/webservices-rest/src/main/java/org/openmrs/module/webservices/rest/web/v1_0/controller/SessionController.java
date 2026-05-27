/**
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * OpenMRS is also distributed under the terms of the Healthcare Disclaimer located at
 * http://openmrs.org/license.
 */
package org.openmrs.module.webservices.rest.web.v1_0.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openmrs.Location;
import org.openmrs.Privilege;
import org.openmrs.Role;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.util.LocaleUtility;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SessionController {

  @GetMapping({"/rest/v1/session", "/ws/rest/v1/session"})
  public Map<String, Object> session(HttpServletRequest request) {
    Map<String, Object> response = new LinkedHashMap<>();
    boolean authenticated = Context.isAuthenticated();
    response.put("authenticated", authenticated);
    response.put("locale", Context.getLocale().toString());
    response.put("allowedLocales", localeSpecifications());

    User authenticatedUser = Context.getAuthenticatedUser();
    if (authenticatedUser != null) {
      response.put("sessionId", request.getSession(true).getId());
      response.put("sessionLocation", locationReference(Context.getUserContext().getLocation()));
      Map<String, Object> user = new LinkedHashMap<>();
      user.put("uuid", authenticatedUser.getUuid());
      user.put("systemId", authenticatedUser.getSystemId());
      user.put("username", authenticatedUser.getUsername());
      user.put("display", authenticatedUser.getDisplayString());
      user.put("userProperties", new LinkedHashMap<>(authenticatedUser.getUserProperties()));
      user.put("privileges", privileges(authenticatedUser));
      user.put("roles", roles(authenticatedUser));
      response.put("user", user);
    }

    return response;
  }

  private List<String> localeSpecifications() {
    return LocaleUtility.getLocalesInOrder().stream().map(Object::toString).toList();
  }

  private List<Map<String, Object>> privileges(User user) {
    return user.getPrivileges().stream()
        .sorted(Comparator.comparing(Privilege::getPrivilege))
        .map(this::privilegeReference)
        .toList();
  }

  private Map<String, Object> privilegeReference(Privilege privilege) {
    Map<String, Object> reference = new LinkedHashMap<>();
    reference.put("name", privilege.getPrivilege());
    reference.put("display", privilege.getPrivilege());
    return reference;
  }

  private List<Map<String, Object>> roles(User user) {
    return user.getAllRoles().stream()
        .sorted(Comparator.comparing(Role::getRole))
        .map(this::roleReference)
        .toList();
  }

  private Map<String, Object> roleReference(Role role) {
    Map<String, Object> reference = new LinkedHashMap<>();
    reference.put("uuid", role.getUuid());
    reference.put("name", role.getRole());
    reference.put("display", role.getRole());
    return reference;
  }

  private Map<String, Object> locationReference(Location location) {
    if (location == null) {
      return null;
    }

    Map<String, Object> reference = new LinkedHashMap<>();
    reference.put("uuid", location.getUuid());
    reference.put("display", location.getDisplayString());
    reference.put("name", location.getName());
    return reference;
  }
}
