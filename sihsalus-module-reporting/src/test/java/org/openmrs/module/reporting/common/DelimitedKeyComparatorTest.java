package org.openmrs.module.reporting.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DelimitedKeyComparatorTest {

  @Test
  void compareSortsNumericSegmentsNumerically() {
    List<String> keys = new ArrayList<>(List.of("1.10", "1.2", "1.B", "10.A", "1.A"));

    keys.sort(new DelimitedKeyComparator());

    assertEquals(List.of("1.2", "1.10", "1.A", "1.B", "10.A"), keys);
  }

  @Test
  void compareSortsShorterEqualPrefixesFirst() {
    List<String> keys = new ArrayList<>(List.of("1.2.1", "1.2"));

    keys.sort(new DelimitedKeyComparator());

    assertEquals(List.of("1.2", "1.2.1"), keys);
  }
}
