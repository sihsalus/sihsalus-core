package org.openmrs.module.reporting.common;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class DateRangeTest {

  @Test
  void parseRejectsRangesWithoutStartAndEndParts() {
    assertThrows(
        IllegalArgumentException.class, () -> DateRange.parse("[2026-01-01)", "yyyy-MM-dd", "*"));
  }

  @Test
  void parseAllowsEmptyEndWhenEmptyStringRepresentsNull() {
    DateRange range = DateRange.parse("[2026-01-01,)", "yyyy-MM-dd", "");

    assertNull(range.getEndDate());
  }
}
