/**
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * OpenMRS is also distributed under the terms of the Healthcare Disclaimer located at
 * http://openmrs.org/license.
 *
 * <p>Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS graphic logo is a
 * trademark of OpenMRS Inc.
 */
package org.openmrs.module.fua;

import org.springframework.stereotype.Component;

/** Contains module's config. */
@Component("fua.FuaConfig")
public class FuaConfig {

  public static final String MODULE_PRIVILEGE = "Fua Privilege";

  public static final String READ_FUA_PRIVILEGE = "Read Fua";

  public static final String MANAGE_FUA_PRIVILEGE = "Manage Fua";

  public static final String DELETE_FUA_PRIVILEGE = "Delete Fua";

  public static final String UPDATE_FUA_PRIVILEGE = "Update Fua";

  // Global Property for FUA Generator microservice URL
  public static final String FUA_GENERATOR_URL_GP = "fua.generator.url";

  public static final String FUA_GENERATOR_URL_DEFAULT = "http://fua-generator:3000";

  public static final String FUA_GENERATOR_IDENTIFIER = "fua.identifier";
}
