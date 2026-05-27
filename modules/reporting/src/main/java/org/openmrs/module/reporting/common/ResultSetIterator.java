package org.openmrs.module.reporting.common;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import org.openmrs.api.APIException;
import org.openmrs.module.reporting.dataset.DataSetColumn;
import org.openmrs.module.reporting.dataset.DataSetRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResultSetIterator implements Iterator<DataSetRow> {

  private static final Logger log = LoggerFactory.getLogger(ResultSetIterator.class);

  private ResultSet resultSet;
  private List<DataSetColumn> columns;
  private Connection connection;
  private Statement statement;
  private boolean isNextUsed = true;
  private boolean hasNext = true;

  public ResultSetIterator(ResultSet resultSet) throws SQLException {
    this.resultSet = resultSet;
    try {
      this.statement = resultSet.getStatement();
      this.connection = statement.getConnection();
      this.createDataSetColumns(resultSet.getMetaData());
    } catch (SQLException e) {
      closeConnection();
      throw e;
    }
  }

  @Override
  public boolean hasNext() {
    if (isNextUsed && hasNext) {
      isNextUsed = false;
      return rawNext();
    }
    return hasNext;
  }

  @Override
  public DataSetRow next() throws NoSuchElementException {
    try {
      if (isNextUsed) {
        if (rawNext()) {
          return createDataSetRow();
        } else {
          throw new NoSuchElementException("No more rows are available");
        }
      } else {
        isNextUsed = true;
        return createDataSetRow();
      }
    } catch (SQLException e) {
      closeConnection();
      throw new APIException("Failed to fetch the next result from the database.", e);
    }
  }

  private boolean rawNext() {
    try {
      if (resultSet.next()) {
        return true;
      } else {
        hasNext = false;
        closeConnection();
        return false;
      }
    } catch (SQLException e) {
      closeConnection();
      throw new APIException("Failed to fetch the next result from the database.", e);
    }
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }

  private DataSetRow createDataSetRow() throws SQLException {
    DataSetRow dataSetRow = new DataSetRow();
    for (int i = 0; i < columns.size(); i++) {
      dataSetRow.addColumnValue(columns.get(i), resultSet.getObject(i + 1));
    }
    return dataSetRow;
  }

  public void closeConnection() {
    closeResultSet();
    closeStatement();
    closeDatabaseConnection();
  }

  private void closeResultSet() {
    try {
      if (resultSet != null) {
        resultSet.close();
      }
    } catch (Exception ex) {
      log.warn("Failed to close ResultSetIterator result set.", ex);
    } finally {
      resultSet = null;
    }
  }

  private void closeStatement() {
    try {
      if (statement != null) {
        statement.close();
      }
    } catch (Exception ex) {
      log.warn("Failed to close ResultSetIterator statement.", ex);
    } finally {
      statement = null;
    }
  }

  private void closeDatabaseConnection() {
    try {
      if (connection != null) {
        connection.close();
      }
    } catch (Exception ex) {
      log.warn("Failed to close ResultSetIterator connection.", ex);
    } finally {
      connection = null;
    }
  }

  public List<DataSetColumn> getColumns() {
    return columns;
  }

  private void createDataSetColumns(ResultSetMetaData metadata) throws SQLException {
    columns = new LinkedList<DataSetColumn>();
    for (int i = 1; i <= metadata.getColumnCount(); i++) {
      String columnName = metadata.getColumnLabel(i);
      columns.add(new DataSetColumn(columnName, columnName, Object.class));
    }
  }
}
