/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.reportingrest.web;

import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.ContextAuthenticationException;

public final class ReportingRestPrivileges {

    public static final String ADD_REPORT_OBJECTS = "Add Report Objects";

    public static final String ADD_REPORTS = "Add Reports";

    public static final String DELETE_REPORT_OBJECTS = "Delete Report Objects";

    public static final String DELETE_REPORTS = "Delete Reports";

    public static final String EDIT_REPORT_OBJECTS = "Edit Report Objects";

    public static final String VIEW_REPORT_OBJECTS = "View Report Objects";

    public static final String VIEW_REPORTS = "View Reports";

    private ReportingRestPrivileges() {
    }

    public static void requireAddReportObjects() {
        requirePrivilege(ADD_REPORT_OBJECTS);
    }

    public static void requireAddReports() {
        requirePrivilege(ADD_REPORTS);
    }

    public static void requireDeleteReportObjects() {
        requirePrivilege(DELETE_REPORT_OBJECTS);
    }

    public static void requireDeleteReports() {
        requirePrivilege(DELETE_REPORTS);
    }

    public static void requireEditReportObjects() {
        requirePrivilege(EDIT_REPORT_OBJECTS);
    }

    public static void requireViewReportObjects() {
        requirePrivilege(VIEW_REPORT_OBJECTS);
    }

    public static void requireViewReports() {
        requirePrivilege(VIEW_REPORTS);
    }

    private static void requirePrivilege(String privilege) {
        try {
            Context.requirePrivilege(privilege);
        } catch (ContextAuthenticationException e) {
            throw new APIAuthenticationException(e.getMessage(), e);
        }
    }
}
