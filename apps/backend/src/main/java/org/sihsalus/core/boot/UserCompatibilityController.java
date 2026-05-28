package org.sihsalus.core.boot;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openmrs.Person;
import org.openmrs.Privilege;
import org.openmrs.Role;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class UserCompatibilityController {

  @GetMapping({"/rest/v1/user/{uuid}", "/ws/rest/v1/user/{uuid}"})
  Map<String, Object> user(@PathVariable("uuid") String uuid) {
    User user = Context.getUserService().getUserByUuid(uuid);
    if (user == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + uuid);
    }
    return user(user, true);
  }

  @GetMapping({"/rest/v1/user", "/ws/rest/v1/user"})
  Map<String, Object> users(
      @RequestParam(value = "q", required = false) String query,
      @RequestParam(value = "startIndex", required = false, defaultValue = "0") int startIndex,
      @RequestParam(value = "limit", required = false, defaultValue = "50") int limit) {
    int safeStartIndex = Math.max(0, startIndex);
    int safeLimit = Math.max(0, Math.min(limit, 100));
    List<Map<String, Object>> results =
        Context.getUserService().getUsers(query, null, false, safeStartIndex, safeLimit).stream()
            .map(user -> user(user, false))
            .toList();

    return Map.of("results", results);
  }

  private Map<String, Object> user(User user, boolean includePrivileges) {
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("uuid", user.getUuid());
    response.put("display", user.getDisplayString());
    response.put("username", user.getUsername());
    response.put("systemId", user.getSystemId());
    response.put("person", personReference(user.getPerson()));
    response.put("roles", roles(user));
    response.put("userProperties", new LinkedHashMap<>(user.getUserProperties()));
    response.put("retired", Boolean.TRUE.equals(user.getRetired()));
    response.put("resourceVersion", "1.8");
    if (includePrivileges) {
      response.put("privileges", privileges(user));
    }
    return response;
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

  private List<Map<String, Object>> privileges(User user) {
    Collection<Privilege> privileges =
        user.isSuperUser() ? Context.getUserService().getAllPrivileges() : user.getPrivileges();
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
}
