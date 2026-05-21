/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.patientflags.evaluator;

import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Cohort;
import org.openmrs.Patient;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.module.patientflags.Flag;
import org.openmrs.module.patientflags.FlagValidationResult;

/**
 * A FlagEvaluator that takes SQL statement as it's criteria. The SQL statement must contain the
 * string "*.patient_id" so that the evaluator knows how to modify the SQL statement to operate on a
 * single patient when required.
 */
public class SQLFlagEvaluator implements FlagEvaluator {

	private Log log = LogFactory.getLog(this.getClass());

	/**
	 * @see org.openmrs.module.patientflags.evaluator.FlagEvaluator#eval(Flag, Patient, Map<Object, Object>)
	 */
	public Boolean eval(Flag flag, Patient patient, Map<Object, Object> context) {

		if(patient.getVoided())
			throw new APIException("Unable to evaluate SQL flag " + flag.getName() + " against voided patient");

		String criteria = flag.getCriteria();

		String patientIdColumn = findPatientIdColumn(criteria);
		if (patientIdColumn == null) {
			throw new APIException("Unable to evaluate SQL flag " + flag.getName() + " without patient_id criteria");
		}

		// since we are going to append a where/and to the end of this sql statement, we need to trim off trailing ";" and any trailing whitespace
		criteria = stripTrailingStatementTerminator(criteria);

		// create the criteria for a single patient by appending a "where" or "and" clause
		String toEval = criteria + (containsIgnoreCase(criteria, "where") ? " and " : " where ") + patientIdColumn + " = "
			+ patient.getPatientId();

		try {
			Context.addProxyPrivilege("SQL Level Access");
			List<List<Object>> resultSet = Context.getAdministrationService().executeSQL(toEval, true);
			// if the list is empty, return false, otherwise, return true
			return !resultSet.isEmpty();
		}
		catch (Exception e) {
			throw new APIException("Unable to evaluate SQL Flag " + flag.getName() + ", " + e.getLocalizedMessage(), e);
		}
		finally{
			Context.removeProxyPrivilege("SQL Level Access");
		}
	}

	/**
	 * @see org.openmrs.module.patientflags.evaluator.FlagEvaluator#eval(Flag, Cohort, Map<Object, Object>)
	 */
	public Cohort evalCohort(Flag flag, Cohort cohort, Map<Object, Object> context) {

		List<List<Object>> resultSet;

		try {
			Context.addProxyPrivilege("SQL Level Access");
			resultSet = Context.getAdministrationService().executeSQL(flag.getCriteria(), true);
		}
		catch (Exception e) {
			throw new APIException("Unable to evaluate SQL Flag " + flag.getName() + ", " + e.getLocalizedMessage(), e);
		}
		finally{
			Context.removeProxyPrivilege("SQL Level Access");
		}

		Cohort resultCohort = new Cohort();

		for (List<Object> row : resultSet) {
			Integer patient_id = (Integer) row.get(0);

			// only add patients that haven't been voided
			if(!Context.getPatientService().getPatient(patient_id).getVoided())
				resultCohort.addMember((Integer) row.get(0));
		}

		if (cohort != null) {
			resultCohort = Cohort.intersect(resultCohort, cohort);
		}

		return resultCohort;
	}

	/**
	 * @see org.openmrs.module.patientflags.evaluator.FlagEvaluator#validate(Flag)
	 */
	public FlagValidationResult validate(Flag flag) {

		String criteria = flag.getCriteria();

		// if the *.patient_id pattern cannot be found, reject the criteria
		if (findPatientIdColumn(criteria) == null)
			return new FlagValidationResult(false, "patientflags.errors.noPatientIdCriteria");

		// try to execute the query, if it throws an exception, fail
		try {
			// note that unlike the two eval methods, we don't proxy SQL Level Access privilege here
			// because we don't want users without SQL Level Access to be able to create SQL Flags
			Context.getAdministrationService().executeSQL(criteria, true);
		}
		catch (Exception e) {
			return new FlagValidationResult(false, e.getLocalizedMessage());
		}

		// if we've gotten this far, mark the criteria as valid
		return new FlagValidationResult(true);
	}
	/**
	 * @see org.openmrs.module.patientflags.evaluator.FlagEvaluator#evalMessage(Flag, int)
	 */
	public String evalMessage(Flag flag, int patientId) {
		String message = flag.getMessage();

		if(!hasMessagePlaceholder(message)){
			return message;
		}

		log.info("Replacing values in "+message);

		Patient p = Context.getPatientService().getPatient(patientId);
		if(p.getVoided())
			throw new APIException("VOIDED PATIENT");

		String criteria = flag.getCriteria();

		String patientIdColumn = findPatientIdColumn(criteria);
		if (patientIdColumn == null) {
			throw new APIException("Unable to evaluate SQL flag message " + flag.getName() + " without patient_id criteria");
		}

		// since we are going to append a where/and to the end of this sql statement, we need to trim off trailing ";" and any trailing whitespace
		criteria = stripTrailingStatementTerminator(criteria);

		// create the criteria for a single patient by appending a "where" or "and" clause
		String toEval = criteria + (containsIgnoreCase(criteria, "where") ? " and " : " where ") + patientIdColumn + " = "
			+ p.getPatientId();

		try {
			Context.addProxyPrivilege("SQL Level Access");
			List<List<Object>> resultSet = Context.getAdministrationService().executeSQL(toEval, true);
			// list would for sure contain one only one patient
			if(!resultSet.isEmpty()){// empty resultset means no one matched the criteria
				message = replaceMessagePlaceholders(message, resultSet.get(0));
			}
			else {
				log.info("result set empty");
			}
		}
		catch (Exception e) {
			throw new APIException("Unable to evaluate SQL Flag Message" + flag.getName() + ", " + e.getLocalizedMessage(), e);
		}
		finally{
			Context.removeProxyPrivilege("SQL Level Access");
		}

		return message;
	}

	private String findPatientIdColumn(String criteria) {
		if (criteria == null) {
			return null;
		}

		String search = ".patient_id";
		int fromIndex = 0;
		while (fromIndex <= criteria.length() - search.length()) {
			int suffixIndex = indexOfIgnoreCase(criteria, search, fromIndex);
			if (suffixIndex == -1) {
				return null;
			}

			int tableEnd = suffixIndex;
			int tableStart = tableEnd - 1;
			while (tableStart >= 0 && isSqlWordChar(criteria.charAt(tableStart))) {
				tableStart--;
			}
			tableStart++;

			int columnEnd = suffixIndex + search.length();
			boolean hasTableAlias = tableStart < tableEnd;
			boolean hasColumnBoundary = columnEnd == criteria.length() || !isSqlWordChar(criteria.charAt(columnEnd));
			if (hasTableAlias && hasColumnBoundary) {
				return criteria.substring(tableStart, columnEnd);
			}
			fromIndex = columnEnd;
		}
		return null;
	}

	private String stripTrailingStatementTerminator(String criteria) {
		int end = criteria.length();
		while (end > 0 && Character.isWhitespace(criteria.charAt(end - 1))) {
			end--;
		}
		if (end > 0 && criteria.charAt(end - 1) == ';') {
			end--;
		}
		while (end > 0 && Character.isWhitespace(criteria.charAt(end - 1))) {
			end--;
		}
		return criteria.substring(0, end);
	}

	private boolean containsIgnoreCase(String value, String search) {
		return indexOfIgnoreCase(value, search, 0) != -1;
	}

	private int indexOfIgnoreCase(String value, String search, int fromIndex) {
		if (value == null || search == null) {
			return -1;
		}
		if (search.length() == 0) {
			return fromIndex <= value.length() ? fromIndex : -1;
		}
		for (int i = Math.max(0, fromIndex); i <= value.length() - search.length(); i++) {
			if (value.regionMatches(true, i, search, 0, search.length())) {
				return i;
			}
		}
		return -1;
	}

	private boolean hasMessagePlaceholder(String message) {
		if (message == null) {
			return false;
		}
		int fromIndex = 0;
		while (fromIndex < message.length()) {
			int start = message.indexOf("${", fromIndex);
			if (start == -1) {
				return false;
			}
			if (findPlaceholderEnd(message, start) != -1) {
				return true;
			}
			fromIndex = start + 2;
		}
		return false;
	}

	private String replaceMessagePlaceholders(String message, List<Object> row) {
		StringBuilder replaced = new StringBuilder();
		int fromIndex = 0;
		while (fromIndex < message.length()) {
			int start = message.indexOf("${", fromIndex);
			if (start == -1) {
				replaced.append(message.substring(fromIndex));
				break;
			}
			replaced.append(message.substring(fromIndex, start));
			int end = findPlaceholderEnd(message, start);
			if (end == -1) {
				replaced.append("${");
				fromIndex = start + 2;
				continue;
			}

			int rowIndex = parsePlaceholderIndex(message, start, end);
			Object value = rowIndex < row.size() ? row.get(rowIndex) : null;
			if (value == null) {
				replaced.append(message.substring(start, end + 1));
			} else {
				replaced.append(value);
				log.info("Replaced " + message.substring(start, end + 1) + " ON " + rowIndex);
			}
			fromIndex = end + 1;
		}
		return replaced.toString();
	}

	private int findPlaceholderEnd(String message, int start) {
		int firstDigit = start + 2;
		if (firstDigit >= message.length() || !isAsciiDigit(message.charAt(firstDigit))) {
			return -1;
		}

		int index = firstDigit + 1;
		if (index < message.length() && isAsciiDigit(message.charAt(index))) {
			index++;
		}

		return index < message.length() && message.charAt(index) == '}' ? index : -1;
	}

	private int parsePlaceholderIndex(String message, int start, int end) {
		int index = 0;
		for (int i = start + 2; i < end; i++) {
			index = (index * 10) + (message.charAt(i) - '0');
		}
		return index;
	}

	private boolean isSqlWordChar(char ch) {
		return (ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9') || ch == '_';
	}

	private boolean isAsciiDigit(char ch) {
		return ch >= '0' && ch <= '9';
	}
}
