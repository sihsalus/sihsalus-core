package org.openmrs.module.reporting.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
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

  @Test
  void executeSqlRejectsNonReadOnlyStatementsBeforePreparingSql() {
    RecordingConnection recordingConnection = new RecordingConnection();
    SqlRunner sqlRunner = new SqlRunner(recordingConnection.proxy());

    SqlResult result = sqlRunner.executeSql("delete from users", null);

    assertFalse(result.getErrors().isEmpty());
    assertFalse(recordingConnection.prepared);
    assertTrue(recordingConnection.rolledBack);
    assertTrue(recordingConnection.autoCommitReset);
  }

  @Test
  void executeSqlClosesResultSetAndStatement() {
    RecordingResultSet recordingResultSet = new RecordingResultSet();
    RecordingPreparedStatement recordingStatement = new RecordingPreparedStatement(recordingResultSet);
    RecordingConnection recordingConnection = new RecordingConnection(recordingStatement);
    SqlRunner sqlRunner = new SqlRunner(recordingConnection.proxy());

    SqlResult result = sqlRunner.executeSql("select name from person", null);

    assertTrue(result.getErrors().isEmpty());
    assertTrue(recordingResultSet.closed);
    assertTrue(recordingStatement.closed);
    assertTrue(recordingConnection.rolledBack);
    assertTrue(recordingConnection.autoCommitReset);
  }

  private static final class RecordingConnection {

    private final RecordingPreparedStatement statement;
    private boolean prepared;
    private boolean rolledBack;
    private boolean autoCommitReset;

    private RecordingConnection() {
      this(null);
    }

    private RecordingConnection(RecordingPreparedStatement statement) {
      this.statement = statement;
    }

    private Connection proxy() {
      return (Connection)
          Proxy.newProxyInstance(
              Connection.class.getClassLoader(),
              new Class[] {Connection.class},
              (proxy, method, args) -> {
                if ("getAutoCommit".equals(method.getName())) {
                  return true;
                }
                if ("setAutoCommit".equals(method.getName())) {
                  autoCommitReset = Boolean.TRUE.equals(args[0]);
                  return null;
                }
                if ("rollback".equals(method.getName())) {
                  rolledBack = true;
                  return null;
                }
                if ("prepareStatement".equals(method.getName())) {
                  prepared = true;
                  return statement.proxy();
                }
                return defaultValue(method.getReturnType());
              });
    }
  }

  private static final class RecordingPreparedStatement {

    private final RecordingResultSet resultSet;
    private boolean closed;

    private RecordingPreparedStatement(RecordingResultSet resultSet) {
      this.resultSet = resultSet;
    }

    private PreparedStatement proxy() {
      return (PreparedStatement)
          Proxy.newProxyInstance(
              PreparedStatement.class.getClassLoader(),
              new Class[] {PreparedStatement.class},
              (proxy, method, args) -> {
                if ("execute".equals(method.getName())) {
                  return true;
                }
                if ("getResultSet".equals(method.getName())) {
                  return resultSet.proxy();
                }
                if ("close".equals(method.getName())) {
                  closed = true;
                  return null;
                }
                return defaultValue(method.getReturnType());
              });
    }
  }

  private static final class RecordingResultSet {

    private boolean closed;

    private ResultSet proxy() {
      return (ResultSet)
          Proxy.newProxyInstance(
              ResultSet.class.getClassLoader(),
              new Class[] {ResultSet.class},
              (proxy, method, args) -> {
                if ("getMetaData".equals(method.getName())) {
                  return metadataProxy();
                }
                if ("next".equals(method.getName())) {
                  return false;
                }
                if ("close".equals(method.getName())) {
                  closed = true;
                  return null;
                }
                return defaultValue(method.getReturnType());
              });
    }

    private ResultSetMetaData metadataProxy() {
      return (ResultSetMetaData)
          Proxy.newProxyInstance(
              ResultSetMetaData.class.getClassLoader(),
              new Class[] {ResultSetMetaData.class},
              (proxy, method, args) -> {
                if ("getColumnCount".equals(method.getName())) {
                  return 1;
                }
                if ("getColumnLabel".equals(method.getName())) {
                  return "name";
                }
                return defaultValue(method.getReturnType());
              });
    }
  }

  private static Object defaultValue(Class<?> returnType) {
    if (returnType == Boolean.TYPE) {
      return false;
    }
    if (returnType == Byte.TYPE) {
      return (byte) 0;
    }
    if (returnType == Short.TYPE) {
      return (short) 0;
    }
    if (returnType == Integer.TYPE) {
      return 0;
    }
    if (returnType == Long.TYPE) {
      return 0L;
    }
    if (returnType == Float.TYPE) {
      return 0F;
    }
    if (returnType == Double.TYPE) {
      return 0D;
    }
    if (returnType == Character.TYPE) {
      return (char) 0;
    }
    return null;
  }
}
