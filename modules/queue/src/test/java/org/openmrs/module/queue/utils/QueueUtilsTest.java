package org.openmrs.module.queue.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import org.junit.jupiter.api.Test;

class QueueUtilsTest {

  private static final Date NULL = null;
  private static final Date AUG_1 = QueueUtils.parseDate("2023-08-01 10:00:00");
  private static final Date AUG_2 = QueueUtils.parseDate("2023-08-02 10:00:00");
  private static final Date AUG_3 = QueueUtils.parseDate("2023-08-03 10:00:00");
  private static final Date AUG_4 = QueueUtils.parseDate("2023-08-04 10:00:00");

  @Test
  void datesOverlapHandlesOpenEndedDates() {
    assertTrue(QueueUtils.datesOverlap(NULL, NULL, NULL, NULL));
    assertFalse(QueueUtils.datesOverlap(NULL, AUG_2, AUG_3, AUG_4));
    assertTrue(QueueUtils.datesOverlap(AUG_1, NULL, AUG_3, AUG_4));
    assertTrue(QueueUtils.datesOverlap(AUG_1, AUG_2, NULL, AUG_4));
    assertFalse(QueueUtils.datesOverlap(AUG_1, AUG_2, AUG_3, NULL));
  }

  @Test
  void datesOverlapIsIndependentOfPeriodOrder() {
    assertFalse(QueueUtils.datesOverlap(AUG_1, AUG_2, AUG_3, AUG_4));
    assertTrue(QueueUtils.datesOverlap(AUG_1, AUG_3, AUG_2, AUG_4));
    assertFalse(QueueUtils.datesOverlap(AUG_3, AUG_4, AUG_1, AUG_2));
    assertTrue(QueueUtils.datesOverlap(AUG_2, AUG_4, AUG_1, AUG_3));
  }

  @Test
  void datesOverlapHandlesClosedBoundaries() {
    assertFalse(QueueUtils.datesOverlap(AUG_1, AUG_2, AUG_3, AUG_4));
    assertFalse(QueueUtils.datesOverlap(AUG_1, AUG_2, AUG_2, AUG_3));
    assertTrue(QueueUtils.datesOverlap(AUG_1, AUG_3, AUG_2, AUG_4));
    assertTrue(QueueUtils.datesOverlap(AUG_1, AUG_4, AUG_2, AUG_3));
    assertTrue(QueueUtils.datesOverlap(AUG_2, AUG_4, AUG_1, AUG_3));
    assertFalse(QueueUtils.datesOverlap(AUG_3, AUG_4, AUG_1, AUG_2));
    assertTrue(QueueUtils.datesOverlap(AUG_1, AUG_2, AUG_1, AUG_3));
  }
}
