package org.openmrs.module.reporting.definition.library;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AllDefinitionLibrariesTest {

  @Test
  void getLibrariesReturnsEmptyUnmodifiableListWhenNoLibrariesAutowired() {
    AllDefinitionLibraries allDefinitionLibraries = new AllDefinitionLibraries();

    assertTrue(allDefinitionLibraries.getLibraries().isEmpty());
    assertThrows(
        UnsupportedOperationException.class, () -> allDefinitionLibraries.getLibraries().add(null));
  }
}
