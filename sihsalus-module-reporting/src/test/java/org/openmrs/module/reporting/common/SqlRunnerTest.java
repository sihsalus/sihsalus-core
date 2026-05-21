package org.openmrs.module.reporting.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SqlRunnerTest {

  @Test
  void parseParametersIntoStatementsEscapesStringValues() {
    SqlRunner sqlRunner = new SqlRunner(null);

    List<String> statements = sqlRunner.parseParametersIntoStatements(Map.of("name", "O'Hara"));

    assertEquals(List.of("set @name='O''Hara'"), statements);
  }

  @Test
  void parseParametersIntoStatementsRejectsUnsafeParameterNames() {
    SqlRunner sqlRunner = new SqlRunner(null);

    assertThrows(
        IllegalArgumentException.class,
        () ->
            sqlRunner.parseParametersIntoStatements(Map.of("name = 1; drop table users; --", "x")));
  }
}
