/**
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * OpenMRS is also distributed under the terms of the Healthcare Disclaimer located at
 * http://openmrs.org/license.
 *
 * <p>Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS graphic logo is a
 * trademark of OpenMRS Inc.
 */
package org.openmrs.logic.op;

/**
 * An operator used within a logical expression.
 *
 * <p>The constants remain for backwards compatibility with existing callers. New code should use
 * {@link ComparisonOperators}, {@link LogicalOperators}, or {@link TransformOperators}.
 */
// codeql[java/constant-interface]
public interface Operator {

  default String getSymbol() {
    return toString();
  }

  // comparison operators
  public static final Operator CONTAINS = ComparisonOperators.CONTAINS;

  public static final Operator EQUALS = ComparisonOperators.EQUALS;

  public static final Operator WITHIN = ComparisonOperators.WITHIN;

  public static final Operator GT = ComparisonOperators.GT;

  public static final Operator GTE = ComparisonOperators.GTE;

  public static final Operator LT = ComparisonOperators.LT;

  public static final Operator LTE = ComparisonOperators.LTE;

  public static final Operator BEFORE = ComparisonOperators.BEFORE;

  public static final Operator AFTER = ComparisonOperators.AFTER;

  public static final Operator IN = ComparisonOperators.IN;

  // weird operator
  public static final Operator ASOF = new AsOf();

  // logical operators
  public static final Operator AND = LogicalOperators.AND;

  public static final Operator OR = LogicalOperators.OR;

  public static final Operator NOT = LogicalOperators.NOT;

  // transform operators
  public static final Operator LAST = TransformOperators.LAST;

  public static final Operator FIRST = TransformOperators.FIRST;

  public static final Operator DISTINCT = TransformOperators.DISTINCT;

  public static final Operator EXISTS = TransformOperators.EXISTS;

  public static final Operator NOT_EXISTS = TransformOperators.NOT_EXISTS;

  public static final Operator COUNT = TransformOperators.COUNT;

  public static final Operator AVERAGE = TransformOperators.AVERAGE;
}
