/**
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * OpenMRS is also distributed under the terms of the Healthcare Disclaimer located at
 * http://openmrs.org/license.
 *
 * <p>Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS graphic logo is a
 * trademark of OpenMRS Inc.
 */
package org.openmrs.module.reporting.report.service.db;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;
import org.openmrs.api.context.Context;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.openmrs.module.reporting.report.definition.service.ReportDefinitionService;

/** A report definition type */
@SuppressWarnings("removal")
public class ReportDefinitionType implements UserType<ReportDefinition> {

  /**
   * @see UserType#assemble(Serializable, Object)
   */
  public ReportDefinition assemble(Serializable cached, Object owner) throws HibernateException {
    if (cached == null) {
      return null;
    }
    return Context.getService(ReportDefinitionService.class).getDefinitionByUuid(cached.toString());
  }

  /**
   * @see UserType#deepCopy(Object)
   */
  public ReportDefinition deepCopy(ReportDefinition value) throws HibernateException {
    return value;
  }

  /**
   * @see UserType#disassemble(Object)
   */
  public Serializable disassemble(ReportDefinition value) throws HibernateException {
    if (value == null) {
      return null;
    }
    return value.getUuid();
  }

  /**
   * @see UserType#equals(Object, Object)
   */
  public boolean equals(ReportDefinition x, ReportDefinition y) throws HibernateException {
    return x != null && x.equals(y);
  }

  /**
   * @see UserType#hashCode(Object)
   */
  public int hashCode(ReportDefinition x) throws HibernateException {
    return x.hashCode();
  }

  /**
   * @see UserType#isMutable()
   */
  public boolean isMutable() {
    return false;
  }

  /**
   * @see UserType#nullSafeGet(ResultSet, String[], Object)
   */
  public ReportDefinition nullSafeGet(
      ResultSet rs, int position, SharedSessionContractImplementor session, Object owner)
      throws SQLException {
    String uuid = rs.getString(position);
    if (uuid == null) {
      return null;
    }
    return Context.getService(ReportDefinitionService.class).getDefinitionByUuid(uuid);
  }

  /**
   * @see UserType#nullSafeSet(PreparedStatement, Object, int, SharedSessionContractImplementor)
   */
  public void nullSafeSet(
      PreparedStatement st,
      ReportDefinition value,
      int index,
      SharedSessionContractImplementor session)
      throws SQLException {
    String val = (value == null ? null : value.getUuid());
    st.setString(index, val);
  }

  /**
   * @see UserType#replace(Object, Object, Object)
   */
  public ReportDefinition replace(ReportDefinition original, ReportDefinition target, Object owner)
      throws HibernateException {
    return original;
  }

  /**
   * @see UserType#returnedClass()
   */
  public Class<ReportDefinition> returnedClass() {
    return ReportDefinition.class;
  }

  /**
   * @see UserType#sqlTypes()
   */
  public int getSqlType() {
    return Types.VARCHAR;
  }
}
