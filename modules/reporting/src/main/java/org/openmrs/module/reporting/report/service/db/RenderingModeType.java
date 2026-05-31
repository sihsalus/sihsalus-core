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
import org.hibernate.HibernateException;
import org.hibernate.metamodel.spi.ValueAccess;
import org.hibernate.usertype.CompositeUserType;
import org.openmrs.module.reporting.report.renderer.RenderingMode;
import org.openmrs.module.reporting.report.renderer.ReportRenderer;

/** Custom User-Type for storing RenderingModes in a single table within 2 columns. */
@SuppressWarnings({"rawtypes", "unchecked"})
public class RenderingModeType implements CompositeUserType<RenderingMode> {

  public static class RenderingModeEmbeddable {

    public Class renderer;

    public String rendererArgument;
  }

  @Override
  public Class<?> embeddable() {
    return RenderingModeEmbeddable.class;
  }

  @Override
  public Class<RenderingMode> returnedClass() {
    return RenderingMode.class;
  }

  @Override
  public boolean isMutable() {
    return true;
  }

  @Override
  public Object getPropertyValue(RenderingMode component, int property) throws HibernateException {
    if (component == null) {
      return null;
    }
    if (property == 0) {
      return component.getRenderer() == null ? null : component.getRenderer().getClass();
    }
    return component.getArgument();
  }

  @Override
  public RenderingMode instantiate(ValueAccess values) {
    Class rendererClass = values.getValue(0, Class.class);
    if (rendererClass == null) {
      return null;
    }

    String argument = values.getValue(1, String.class);
    try {
      ReportRenderer renderer =
          (ReportRenderer) rendererClass.getDeclaredConstructor().newInstance();
      return new RenderingMode(renderer, renderer.getClass().getSimpleName(), argument, null);
    } catch (Exception e) {
      throw new HibernateException(
          "Error instantiating a new reporting renderer from " + rendererClass, e);
    }
  }

  @Override
  public RenderingMode deepCopy(RenderingMode value) throws HibernateException {
    if (value == null) {
      return null;
    }
    return new RenderingMode(
        value.getRenderer(), value.getLabel(), value.getArgument(), value.getSortWeight());
  }

  @Override
  public RenderingMode replace(RenderingMode original, RenderingMode target, Object owner)
      throws HibernateException {
    return original;
  }

  @Override
  public boolean equals(RenderingMode x, RenderingMode y) throws HibernateException {
    return x == y || (x != null && x.equals(y));
  }

  @Override
  public int hashCode(RenderingMode x) throws HibernateException {
    return x == null ? 0 : x.hashCode();
  }

  @Override
  public Serializable disassemble(RenderingMode value) throws HibernateException {
    return value == null ? null : value.getDescriptor();
  }

  @Override
  public RenderingMode assemble(Serializable cached, Object owner) throws HibernateException {
    return cached == null ? null : new RenderingMode(cached.toString());
  }
}
