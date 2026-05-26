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

import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.api.context.Context;
import org.openmrs.module.billing.ModuleSettings;
import org.openmrs.module.billing.api.model.Timesheet;
import org.openmrs.module.billing.api.util.PrivilegeConstants;
import org.openmrs.module.billing.api.util.TimesheetUtil;
import org.openmrs.module.billing.web.CashierWebConstants;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/** Type of a RestController to check up setting values in the Cashier Module Settings. */
@Controller(value = "cashierModuleSettings")
@RequestMapping(CashierWebConstants.MODULE_SETTINGS_PAGE)
public class CashierModuleSettingsController {

  private static final String TIMESHEET_SETTING = "timesheet";

  private static final Set<String> BILLING_SETTINGS =
      Set.of(
          ModuleSettings.ADJUSTMENT_REASON_FIELD,
          ModuleSettings.ALLOW_BILL_ADJUSTMENT,
          ModuleSettings.AUTOFILL_PAYMENT_AMOUNT,
          ModuleSettings.CASHIER_SHIFT_REPORT_ID_PROPERTY,
          ModuleSettings.DAILY_SHIFT_SUMMARY_REPORT_ID_PROPERTY,
          ModuleSettings.DEPARTMENT_COLLECTIONS_REPORT_ID_PROPERTY,
          ModuleSettings.DEPARTMENT_REVENUE_REPORT_ID_PROPERTY,
          ModuleSettings.DISCOUNT_ENABLED,
          ModuleSettings.PATIENT_DASHBOARD_2_BILL_COUNT,
          ModuleSettings.PAYMENTS_BY_PAYMENT_MODE_REPORT_ID_PROPERTY,
          ModuleSettings.RECEIPT_REPORT_ID_PROPERTY,
          ModuleSettings.REFUND_ENABLED,
          ModuleSettings.ROUNDING_DEPT_ID,
          ModuleSettings.ROUNDING_ITEM_ID,
          ModuleSettings.ROUNDING_MODE_PROPERTY,
          ModuleSettings.ROUND_TO_NEAREST_PROPERTY,
          ModuleSettings.SHIFT_SUMMARY_REPORT_ID_PROPERTY,
          ModuleSettings.SYSTEM_RECEIPT_NUMBER_GENERATOR,
          ModuleSettings.TIMESHEET_REQUIRED_PROPERTY);

  public CashierModuleSettingsController() {}

  @ResponseBody
  @RequestMapping(method = RequestMethod.GET)
  public SimpleObject get(@RequestParam("setting") String setting) {
    SimpleObject results = new SimpleObject();
    if (StringUtils.isNotEmpty(setting)) {
      if (StringUtils.equalsIgnoreCase(setting, TIMESHEET_SETTING)) {
        Context.requirePrivilege(PrivilegeConstants.VIEW_TIMESHEETS);
        results.put("isTimeSheetRequired", TimesheetUtil.isTimesheetRequired());
        Timesheet currentTimesheet = getCurrentTimesheet();
        if (currentTimesheet != null) {
          SimpleObject cashPoint = new SimpleObject();
          cashPoint.put("name", currentTimesheet.getCashPoint().getName());
          cashPoint.put("uuid", currentTimesheet.getCashPoint().getUuid());
          results.put("cashPoint", cashPoint);
          results.put("cashier", currentTimesheet.getCashier().getName());
        }
      } else {
        if (BILLING_SETTINGS.contains(setting)) {
          Context.requirePrivilege(PrivilegeConstants.VIEW_METADATA);
        } else {
          Context.requirePrivilege(org.openmrs.util.PrivilegeConstants.MANAGE_GLOBAL_PROPERTIES);
        }
        results.put("results", Context.getAdministrationService().getGlobalProperty(setting));
      }
    }
    return results;
  }

  private Timesheet getCurrentTimesheet() {
    Timesheet timesheet;
    try {
      timesheet = TimesheetUtil.getCurrentTimesheet();
    } catch (Exception e) {
      timesheet = null;
    }
    return timesheet;
  }
}
