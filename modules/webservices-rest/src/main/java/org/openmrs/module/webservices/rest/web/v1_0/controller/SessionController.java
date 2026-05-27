/**
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * OpenMRS is also distributed under the terms of the Healthcare Disclaimer located at
 * http://openmrs.org/license.
 */
package org.openmrs.module.webservices.rest.web.v1_0.controller;

import java.util.LinkedHashMap;
import java.util.Map;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SessionController {

  @GetMapping({"/rest/v1/session", "/ws/rest/v1/session"})
  public Map<String, Object> session() {
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("authenticated", Context.isAuthenticated());

    User authenticatedUser = Context.getAuthenticatedUser();
    if (authenticatedUser != null) {
      Map<String, Object> user = new LinkedHashMap<>();
      user.put("uuid", authenticatedUser.getUuid());
      user.put("username", authenticatedUser.getUsername());
      user.put("display", authenticatedUser.getDisplayString());
      response.put("user", user);
    }

    return response;
  }
}
