/**
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * OpenMRS is also distributed under the terms of the Healthcare Disclaimer located at
 * http://openmrs.org/license.
 */
package org.openmrs.module.webservices.rest.web.v1_0.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openmrs.Location;
import org.openmrs.Person;
import org.openmrs.Privilege;
import org.openmrs.Role;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.util.LocaleUtility;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class SessionController {

  @GetMapping({"/rest/v1/session", "/ws/rest/v1/session"})
  public Map<String, Object> session(HttpServletRequest request) {
    return sessionResponse(request);
  }

  @PostMapping({"/rest/v1/session", "/ws/rest/v1/session"})
  public Map<String, Object> updateSession(
      HttpServletRequest request, @RequestBody(required = false) Map<String, Object> body) {
    if (Context.isAuthenticated() && body != null) {
      Object sessionLocation =
          body.containsKey("sessionLocation") ? body.get("sessionLocation") : body.get("location");
      if (sessionLocation != null) {
        Context.getUserContext().setLocation(resolveLocation(sessionLocation));
      }
    }

    return sessionResponse(request);
  }

  private Map<String, Object> sessionResponse(HttpServletRequest request) {
    Map<String, Object> response = new LinkedHashMap<>();
    boolean authenticated = Context.isAuthenticated();
    response.put("authenticated", authenticated);
    response.put("locale", Context.getLocale().toString());
    response.put("allowedLocales", localeSpecifications());

    Location sessionLocation = Context.getUserContext().getLocation();
    if (sessionLocation != null) {
      Map<String, Object> locationMap = new LinkedHashMap<>();
      locationMap.put("uuid", sessionLocation.getUuid());
      locationMap.put("display", sessionLocation.getName());
      response.put("sessionLocation", locationMap);
    } else {
      response.put("sessionLocation", null);
    }

    User authenticatedUser = Context.getAuthenticatedUser();
    if (authenticatedUser != null) {
      response.put("sessionId", request.getSession(true).getId());
      response.put("sessionLocation", locationReference(Context.getUserContext().getLocation()));
      Map<String, Object> user = new LinkedHashMap<>();
      user.put("uuid", authenticatedUser.getUuid());
      user.put("systemId", authenticatedUser.getSystemId());
      user.put("username", authenticatedUser.getUsername());
      user.put("display", authenticatedUser.getDisplayString());
      user.put("person", personReference(authenticatedUser.getPerson()));
      user.put("userProperties", new LinkedHashMap<>(authenticatedUser.getUserProperties()));
      user.put("privileges", privileges(authenticatedUser));
      user.put("roles", roles(authenticatedUser));
      response.put("user", user);
    }

    return response;
  }

  private Location resolveLocation(Object value) {
    String identifier = locationIdentifier(value);
    if (identifier == null) {
      return null;
    }

    Location location = Context.getLocationService().getLocationByUuid(identifier);
    if (location == null && isInteger(identifier)) {
      location = Context.getLocationService().getLocation(Integer.parseInt(identifier));
    }
    if (location == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Unknown session location: " + identifier);
    }

    return location;
  }

  private String locationIdentifier(Object value) {
    if (value instanceof Map<?, ?> map) {
      Object uuid = map.get("uuid");
      if (uuid != null) {
        return normalizeLocationIdentifier(uuid.toString());
      }
      Object id = map.get("id");
      if (id != null) {
        return normalizeLocationIdentifier(id.toString());
      }
      Object reference = map.get("reference");
      return reference == null ? null : normalizeLocationIdentifier(reference.toString());
    }

    return normalizeLocationIdentifier(value.toString());
  }

  private String normalizeLocationIdentifier(String value) {
    String identifier = blankToNull(value);
    if (identifier != null && identifier.startsWith("Location/")) {
      return blankToNull(identifier.substring("Location/".length()));
    }
    return identifier;
  }

  private String blankToNull(String value) {
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private boolean isInteger(String value) {
    for (int i = 0; i < value.length(); i++) {
      if (!Character.isDigit(value.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  private List<String> localeSpecifications() {
    return LocaleUtility.getLocalesInOrder().stream().map(Object::toString).toList();
  }

  private List<Map<String, Object>> privileges(User user) {
    Collection<Privilege> privileges;
    if (user.isSuperUser()) {
      privileges = Context.getUserService().getAllPrivileges();
    } else {
      Set<Privilege> effectivePrivileges = new HashSet<>();
      for (Role role : sessionRoles(user)) {
        if (role.getPrivileges() != null) {
          effectivePrivileges.addAll(role.getPrivileges());
        }
      }
      privileges = effectivePrivileges;
    }

    return privileges.stream()
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
    return sessionRoles(user).stream()
        .sorted(Comparator.comparing(Role::getRole))
        .map(this::roleReference)
        .toList();
  }

  private Collection<Role> sessionRoles(User user) {
    try {
      return Context.getUserContext().getAllRoles(user);
    } catch (Exception exception) {
      return user.getAllRoles();
    }
  }

  private Map<String, Object> roleReference(Role role) {
    Map<String, Object> reference = new LinkedHashMap<>();
    reference.put("uuid", role.getUuid());
    reference.put("name", role.getRole());
    reference.put("display", role.getRole());
    return reference;
  }

  private Map<String, Object> personReference(Person person) {
    if (person == null) {
      return null;
    }

    Map<String, Object> reference = new LinkedHashMap<>();
    reference.put("uuid", person.getUuid());
    reference.put(
        "display",
        person.getPersonName() == null ? person.getUuid() : person.getPersonName().toString());
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
