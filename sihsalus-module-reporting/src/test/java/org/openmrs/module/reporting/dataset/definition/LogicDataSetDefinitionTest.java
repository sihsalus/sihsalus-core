package org.openmrs.module.reporting.dataset.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.openmrs.logic.result.Result;
import org.openmrs.module.reporting.dataset.definition.LogicDataSetDefinition.DecodeFormatter;

class LogicDataSetDefinitionTest {

  @Test
  void decodeFormatterAllowsColonInsideReplacementValue() {
    DecodeFormatter formatter = new DecodeFormatter("code:label:with:colon");

    assertEquals("label:with:colon", formatter.format(new Result("code")));
  }
}
