/**
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * OpenMRS is also distributed under the terms of the Healthcare Disclaimer located at
 * http://openmrs.org/license.
 *
 * <p>Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS graphic logo is a
 * trademark of OpenMRS Inc.
 */
package org.openmrs.util;

import java.io.Serializable;
import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;
import org.openmrs.OpenmrsMetadata;

/**
 * A comparator that sorts first based on non-retired, and second based on name. (Locale is
 * currently not used, but will be when we add the ability to localize metadata.)
 *
 * @since 1.7
 */
public class MetadataComparator implements Comparator<OpenmrsMetadata>, Serializable {

  private static final long serialVersionUID = 1L;

  private final Collator collator;

  /**
   * @param locale
   */
  public MetadataComparator(Locale locale) {
    collator = Collator.getInstance(locale == null ? Locale.getDefault() : locale);
  }

  /**
   * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
   */
  @Override
  public int compare(OpenmrsMetadata left, OpenmrsMetadata right) {
    int temp = OpenmrsUtil.compareWithNullAsLowest(left.getRetired(), right.getRetired());
    if (temp == 0) {
      temp = compareNames(left.getName(), right.getName());
    }
    return temp;
  }

  private int compareNames(String leftName, String rightName) {
    if (leftName == null || rightName == null) {
      return OpenmrsUtil.compareWithNullAsLowest(leftName, rightName);
    }
    return collator.compare(leftName, rightName);
  }
}
