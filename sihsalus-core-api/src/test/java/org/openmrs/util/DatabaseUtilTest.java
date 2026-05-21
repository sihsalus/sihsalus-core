package org.openmrs.util;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class DatabaseUtilTest {

  @Test
  void getUniqueNonNullColumnValuesRejectsUnsafeColumnIdentifiers() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            DatabaseUtil.getUniqueNonNullColumnValues(
                "uuid; drop table users", "users", String.class, null));
  }

  @Test
  void getUniqueNonNullColumnValuesRejectsUnsafeTableIdentifiers() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            DatabaseUtil.getUniqueNonNullColumnValues(
                "uuid", "users where 1=1", String.class, null));
  }
}
