package org.openmrs.module.patientflags.evaluator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SQLFlagEvaluatorTest {

  private final SQLFlagEvaluator evaluator = new SQLFlagEvaluator();

  @Test
  void findPatientIdColumnFindsQualifiedColumnCaseInsensitively() {
    assertEquals(
        "e.Patient_ID", evaluator.findPatientIdColumn("select e.Patient_ID from encounter e"));
  }

  @Test
  void findPatientIdColumnRejectsUnqualifiedColumn() {
    assertNull(evaluator.findPatientIdColumn("select patient_id from patient"));
  }

  @Test
  void stripTrailingStatementTerminatorRemovesOnlyFinalDelimiter() {
    assertEquals(
        "select e.patient_id from encounter e",
        evaluator.stripTrailingStatementTerminator("select e.patient_id from encounter e;  "));
  }

  @Test
  void hasMessagePlaceholderOnlyAcceptsOneOrTwoDigitPlaceholders() {
    assertTrue(evaluator.hasMessagePlaceholder("value ${1}"));
    assertTrue(evaluator.hasMessagePlaceholder("value ${12}"));
    assertFalse(evaluator.hasMessagePlaceholder("value ${123}"));
    assertFalse(evaluator.hasMessagePlaceholder("value ${name}"));
  }

  @Test
  void replaceMessagePlaceholdersLeavesInvalidOrMissingValuesUntouched() {
    String message =
        evaluator.replaceMessagePlaceholders("A ${0} B ${2} C ${bad}", List.of("one", "two"));

    assertEquals("A one B ${2} C ${bad}", message);
  }
}
