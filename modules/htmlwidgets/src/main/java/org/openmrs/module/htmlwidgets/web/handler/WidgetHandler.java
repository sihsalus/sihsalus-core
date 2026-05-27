/**
 * The contents of this file are subject to the OpenMRS Public License Version 1.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://license.openmrs.org
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>Copyright (C) OpenMRS, LLC. All Rights Reserved.
 */
package org.openmrs.module.htmlwidgets.web.handler;

import java.io.IOException;
import java.io.Writer;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.module.htmlwidgets.web.WidgetConfig;

/** Base WidgetHandler class. */
public abstract class WidgetHandler {

  protected static final Log log = LogFactory.getLog(WidgetHandler.class);

  /**
   * This is the main method that should be overridden by subclasses to render the appropriate
   * Widget
   *
   * @param config
   * @param w
   */
  public abstract void render(WidgetConfig config, Writer w) throws IOException;

  /**
   * This is the main method that should be overridden by subclasses to parse an input string to an
   * object
   *
   * @param input
   * @param type
   */
  public abstract Object parse(String input, Class<?> type);

  protected Integer parseInteger(String input) {
    if (StringUtils.isBlank(input)) {
      return null;
    }
    try {
      return Integer.valueOf(input);
    } catch (NumberFormatException e) {
      log.warn("Invalid integer value: " + input, e);
      return null;
    }
  }

  protected Long parseLong(String input) {
    if (StringUtils.isBlank(input)) {
      return null;
    }
    try {
      return Long.valueOf(input);
    } catch (NumberFormatException e) {
      log.warn("Invalid long value: " + input, e);
      return null;
    }
  }

  protected Double parseDouble(String input) {
    if (StringUtils.isBlank(input)) {
      return null;
    }
    try {
      return Double.valueOf(input);
    } catch (NumberFormatException e) {
      log.warn("Invalid double value: " + input, e);
      return null;
    }
  }

  protected Float parseFloat(String input) {
    if (StringUtils.isBlank(input)) {
      return null;
    }
    try {
      return Float.valueOf(input);
    } catch (NumberFormatException e) {
      log.warn("Invalid float value: " + input, e);
      return null;
    }
  }
}
