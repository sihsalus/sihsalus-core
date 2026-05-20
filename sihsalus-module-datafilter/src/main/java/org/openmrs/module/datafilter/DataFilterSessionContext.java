/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 */
package org.openmrs.module.datafilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DataFilterSessionContext {

    private static final Logger log = LoggerFactory.getLogger(DataFilterSessionContext.class);

    private DataFilterSessionContext() {}

    public static void reset() {
        log.debug("Data filter session reset requested; Hibernate 7 runtime filters are not wired in static mode.");
    }
}
