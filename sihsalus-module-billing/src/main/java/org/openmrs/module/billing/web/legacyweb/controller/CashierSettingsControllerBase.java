/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.billing.web.legacyweb.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.openmrs.api.context.Context;
import org.openmrs.module.billing.ModuleSettings;
import org.openmrs.module.billing.api.base.web.WebConstants;
import org.openmrs.module.billing.api.model.CashierSettings;
import org.openmrs.module.billing.api.util.PrivilegeConstants;
import org.openmrs.module.billing.web.base.controller.HeaderController;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/** Base Controller to manage the settings pages. */
public abstract class CashierSettingsControllerBase {

  @RequestMapping(method = RequestMethod.GET)
  public void render(ModelMap modelMap, HttpServletRequest request) {
    Context.requirePrivilege(PrivilegeConstants.MANAGE_METADATA);
    modelMap.addAttribute("cashierSettings", ModuleSettings.loadSettings());
    HeaderController.render(modelMap, request);
  }

  @RequestMapping(method = RequestMethod.POST)
  public void submit(
      HttpServletRequest request, CashierSettings cashierSettings, ModelMap modelMap) {
    Context.requirePrivilege(PrivilegeConstants.MANAGE_METADATA);
    ModuleSettings.saveSettings(cashierSettings);

    HttpSession session = request.getSession();
    session.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "openhmis.cashier.settings.saved");

    render(modelMap, request);
  }
}
