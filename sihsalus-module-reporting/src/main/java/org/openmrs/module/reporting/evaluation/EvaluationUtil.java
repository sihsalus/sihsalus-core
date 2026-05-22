/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.reporting.evaluation;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.module.reporting.common.ObjectUtil;
import org.openmrs.module.reporting.evaluation.caching.Caching;
import org.openmrs.module.reporting.evaluation.caching.CachingStrategy;
import org.openmrs.module.reporting.evaluation.caching.NoCachingStrategy;
import org.openmrs.module.reporting.evaluation.parameter.ParameterException;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides utility methods useful for Evaluation
 */
public class EvaluationUtil {

	private static Log log = LogFactory.getLog(EvaluationUtil.class);

	public static final String EXPRESSION_START = "${";
	public static final String EXPRESSION_END = "}";
	public static final String FORMAT_SEPARATOR = "\\|";

    /**
	 * Returns true if the passed String is an expression that is capable of being evaluated
	 * @param s the String to check
	 * @return true if the passed String is an expression that is capable of being evaluated
	 */
	public static boolean isExpression(String s) {
		return s != null && s.startsWith(EXPRESSION_START) && s.endsWith(EXPRESSION_END);
	}

	/**
	 * Returns the passed String, removing the expression start and end delimiters
	 * @param s the original string
	 * @return the passed String, removing the expression start and end delimiters
	 */
	public static String stripExpression(String s) {
		if (isExpression(s)) {
			s = s.substring(EXPRESSION_START.length(), s.length()-EXPRESSION_END.length());
		}
		return s;
	}

	/**
	 * @see EvaluationUtil#evaluateExpression(String, Map<String, Object>, Class)
	 */
	public static Object evaluateExpression(String expression, EvaluationContext context) throws ParameterException {
		Map<String, Object> params = new HashMap<String, Object>(context.getParameterValues());
		params.putAll(context.getContextValues());
		return evaluateExpression(expression, params);
	}

	/**
	 * This method will parse the passed expression and return a value based on the following
	 * criteria:<br/>
	 * <ul>
	 * <li>Any string that matches a passed parameter will be replaced by the value of that parameter
	 * <li>If this date is followed by an expression, it will attempt to evaluate this by
	 * incrementing/decrementing days/weeks/months/years as specified</li>
	 * <li>Examples: Given 2 parameters:
	 * <ul>
	 * <li>report.startDate = java.util.Date with value of [2007-01-10]
	 * <li>report.gender = "male"
	 * </ul>
	 * The following should result:<br/>
	 * <br/>
	 * <pre>
	 * evaluateExpression("${report.startDate}") -> "2007-01-10" as Date
	 * evaluateExpression("${report.startDate+5d}") -> "2007-01-15" as Date
	 * evaluateExpression("${report.startDate-1w}") -> "2007-01-03" as Date
	 * evaluateExpression("${report.startDate+3m}") -> "2007-04-15" as Date
	 * evaluateExpression("${report.startDate+1y}") -> "2008-01-10" as Date
	 * <pre>
	 * </ul>
	 *
	 * @param expression
	 * @return value for given expression, as an <code>Object</code>
	 * @throws ParameterException
	 */
	public static Object evaluateExpression(String expression, Map<String, Object> parameters) throws ParameterException {
		return evaluateExpression(expression, parameters, EXPRESSION_START, EXPRESSION_END);
	}

	/**
	 */
	public static Object evaluateExpression(String expression, Map<String, Object> parameters,
	                                        String expressionPrefix, String expressionSuffix) throws ParameterException {

		while (expression != null) {
			String newExpression = expression;

			int startIndex = expression.indexOf(expressionPrefix);
			int endIndex = expression.indexOf(expressionSuffix, startIndex+1);
			StringBuilder sb = new StringBuilder();
			if (startIndex != -1 && endIndex != -1) {

				String e = expression.substring(startIndex + expressionPrefix.length(), endIndex);
				Object replacement = evaluateParameterExpression(e, parameters);

				if (startIndex == 0 && endIndex == expression.length()-1) {
					return replacement;
				}

				sb.append(expression.substring(0, startIndex));
				sb.append(ObjectUtil.format(replacement));
				sb.append(expression.substring(endIndex + expressionSuffix.length()));
				newExpression = sb.toString();
			}

			if (newExpression.equals(expression)) {
				return newExpression;
			}
			expression = newExpression;
		}
		return null;
	}

	/**
	 * This can be used to retrieve the parameter name that may be included within a parameter expression
	 * If the expression has no expression-like syntax to remove, it simply returns the expression
	 * For example ${startDate} and ${startDate+1d} would both return "startDate"
	 *
	 * @param expression a parameter expression to check
	 * @return the unadorned parameter value that is contained within the passed expression.
	 */
	public static String parseParameterNameFromExpression(String expression) {
		expression = EvaluationUtil.stripExpression(expression);
		ParsedExpression parsedExpression = parseExpression(expression);
		if (parsedExpression != null) {
			return parsedExpression.parameterName;
		}
		return expression;
	}

	/**
	 * This method will parse the passed expression and return a value based on the following
	 * criteria:<br/>
	 * <ul>
	 * <li>Any string that matches a passed parameter will be replaced by the value of that parameter
	 * <li>If this date is followed by an expression, it will attempt to evaluate this by
	 * incrementing/decrementing days/weeks/months/years as specified</li>
	 * <li>Examples: Given 2 parameters:
	 * <ul>
	 * <li>report.startDate = java.util.Date with value of [2007-01-10]
	 * <li>report.gender = "male"
	 * </ul>
	 * The following should result:<br/>
	 * <br/>
	 * <pre>
	 * evaluateParameterExpression("report.startDate") -> "2007-01-10" as Date
	 * <pre>
	 * </ul>
	 *
	 * @param expression
	 * @return value for given expression, as an <code>Object</code>
	 * @throws org.openmrs.module.reporting.evaluation.parameter.ParameterException
	 */
	public static Object evaluateParameterExpression(String expression, Map<String, Object> parameters) throws ParameterException {

		log.debug("evaluateParameterExpression(): " + expression);

		log.debug("Starting expression: " + expression);
		String[] paramAndFormat = expression.split(FORMAT_SEPARATOR, 2);
		Object paramValueToFormat = null;

        try {
            ParsedExpression parsedExpression = parseExpression(paramAndFormat[0]);
            if (parsedExpression != null) {
                String parameterName = parsedExpression.parameterName;
                paramValueToFormat = parameters.get(parameterName);
                if (paramValueToFormat == null) {
                    log.debug("Looked like an expression but the parameter value is null");
                } else {
                    for (Operation operation : parsedExpression.operations) {
                        char op = operation.operator;
                        String number = operation.number;
                        String unit = operation.unit.toLowerCase();
                        if (paramValueToFormat instanceof Date) {
                            if (op != '+' && op != '-') {
                                throw new IllegalArgumentException("Dates only support the + and - operators");
                            }
                            Integer numAsInt;
                            try {
                                numAsInt = Integer.parseInt(number);
                            } catch (NumberFormatException ex) {
                                throw new IllegalArgumentException("Dates do not support arithmetic with floating-point values");
                            }

                            if (op == '-') {
                                numAsInt = -numAsInt;
                            }
                            if ("w".equals(unit)) {
                                unit = "d";
                                numAsInt *= 7;
                            }
                            if ("ms".equals(unit)) {
                                paramValueToFormat = DateUtils.addMilliseconds((Date) paramValueToFormat, numAsInt);
                            } else if ("s".equals(unit)) {
                                paramValueToFormat = DateUtils.addSeconds((Date) paramValueToFormat, numAsInt);
                            } else if ("h".equals(unit)) {
                                paramValueToFormat = DateUtils.addHours((Date) paramValueToFormat, numAsInt);
                            } else if ("m".equals(unit)) {
                                paramValueToFormat = DateUtils.addMonths((Date) paramValueToFormat, numAsInt);
                            } else if ("y".equals(unit)) {
                                paramValueToFormat = DateUtils.addYears((Date) paramValueToFormat, numAsInt);
                            } else if ("".equals(unit) || "d".equals(unit)) {
                                paramValueToFormat = DateUtils.addDays((Date) paramValueToFormat, numAsInt);
                            } else {
                                throw new IllegalArgumentException("Unknown unit: " + unit);
                            }
                        }
                        else { // assume it's a number
                            if (!"".equals(unit)) {
                                throw new IllegalArgumentException("Can't specify units in a non-date expression");
                            }
                            if (paramValueToFormat instanceof Integer && isWholeNumber(number)) {
                                Integer parsed = Integer.parseInt(number);
                                if (op == '+') {
                                    paramValueToFormat = ((Integer) paramValueToFormat) + parsed;
                                } else if (op == '-') {
                                    paramValueToFormat = ((Integer) paramValueToFormat) - parsed;
                                } else if (op == '*') {
                                    paramValueToFormat = ((Integer) paramValueToFormat) * parsed;
                                } else if (op == '/') {
                                    paramValueToFormat = ((Integer) paramValueToFormat) / parsed;
                                } else {
                                    throw new IllegalArgumentException("Unknown operator " + op);
                                }
                            } else {
                                // since one or both are decimal values, do double arithmetic
                                Double parsed = Double.parseDouble(number);
                                if (op == '+') {
                                    paramValueToFormat = ((Number) paramValueToFormat).doubleValue() + parsed;
                                } else if (op == '-') {
                                    paramValueToFormat = ((Number) paramValueToFormat).doubleValue() - parsed;
                                } else if (op == '*') {
                                    paramValueToFormat = ((Number) paramValueToFormat).doubleValue() * parsed;
                                } else if (op == '/') {
                                    paramValueToFormat = ((Number) paramValueToFormat).doubleValue() / parsed;
                                } else {
                                    throw new IllegalArgumentException("Unknown operator " + op);
                                }
                            }
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            log.debug(e.getMessage());
            throw new ParameterException("Error handling expression: " + paramAndFormat[0], e);
        }

		paramValueToFormat = ObjectUtil.nvl(paramValueToFormat, parameters.get(paramAndFormat[0]));
		if (ObjectUtil.isNull(paramValueToFormat)) {
			if (parameters.containsKey(paramAndFormat[0])) {
				return paramValueToFormat;
			}
			else {
				return expression;
			}
		}
		log.debug("Evaluated to: " + paramValueToFormat);

		// Attempt to format the evaluated value if appropriate
		if (paramAndFormat.length == 2) {
			paramValueToFormat = ObjectUtil.format(paramValueToFormat, paramAndFormat[1]);
		}

		return paramValueToFormat;
	}

	private static ParsedExpression parseExpression(String expression) {
		if (expression == null) {
			return null;
		}

		int index = 0;
		int length = expression.length();
		while (index < length && isParameterNameChar(expression.charAt(index))) {
			index++;
		}
		if (index == 0 || index == length) {
			return null;
		}

		String parameterName = expression.substring(0, index);
		List<Operation> operations = new ArrayList<Operation>();
		while (index < length) {
			while (index < length && Character.isWhitespace(expression.charAt(index))) {
				index++;
			}
			if (index >= length) {
				return null;
			}

			char operator = expression.charAt(index);
			if (operator != '+' && operator != '-' && operator != '*' && operator != '/') {
				return null;
			}
			index++;

			while (index < length && Character.isWhitespace(expression.charAt(index))) {
				index++;
			}

			int numberStart = index;
			while (index < length && isAsciiDigit(expression.charAt(index))) {
				index++;
			}
			if (index < length && expression.charAt(index) == '.') {
				index++;
				int digitsAfterDecimalStart = index;
				while (index < length && isAsciiDigit(expression.charAt(index))) {
					index++;
				}
				if (index == digitsAfterDecimalStart) {
					return null;
				}
			} else if (index == numberStart) {
				return null;
			}

			String number = expression.substring(numberStart, index);
			int unitStart = index;
			while (index < length && Character.isLetter(expression.charAt(index))) {
				index++;
			}
			operations.add(new Operation(operator, number, expression.substring(unitStart, index)));
		}

		return operations.isEmpty() ? null : new ParsedExpression(parameterName, operations);
	}

	private static boolean isParameterNameChar(char ch) {
		return Character.isLetterOrDigit(ch) || ch == '_' || ch == '.';
	}

	private static boolean isWholeNumber(String number) {
		if (number == null || number.length() == 0) {
			return false;
		}
		for (int i = 0; i < number.length(); i++) {
			if (!isAsciiDigit(number.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	private static boolean isAsciiDigit(char ch) {
		return ch >= '0' && ch <= '9';
	}

	private static class ParsedExpression {

		private final String parameterName;

		private final List<Operation> operations;

		private ParsedExpression(String parameterName, List<Operation> operations) {
			this.parameterName = parameterName;
			this.operations = operations;
		}
	}

	private static class Operation {

		private final char operator;

		private final String number;

		private final String unit;

		private Operation(char operator, String number, String unit) {
			this.operator = operator;
			this.number = number;
			this.unit = unit;
		}
	}

	/**
	 * @return the Cache key for the given definition class
	 */
	public static String getCacheKey(Definition definition, EvaluationContext context) {
		String cacheKey = null;
		Caching caching = definition.getClass().getAnnotation(Caching.class);
		if (caching != null && caching.strategy() != NoCachingStrategy.class) {
			try {
				CachingStrategy strategy = caching.strategy().newInstance();
				cacheKey = strategy.getCacheKey(definition, context);
			}
			catch (Exception e) {
				log.warn("An error occurred while attempting to access the cache.", e);
			}
		}
		return cacheKey;
	}
}
