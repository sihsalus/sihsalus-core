/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.reporting.report.service.db;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.metamodel.spi.ValueAccess;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.usertype.ParameterizedType;
import org.openmrs.api.context.Context;
import org.openmrs.module.reporting.definition.DefinitionContext;
import org.openmrs.module.reporting.evaluation.Definition;
import org.openmrs.module.reporting.evaluation.parameter.Mapped;
import org.openmrs.module.reporting.evaluation.parameter.Parameterizable;
import org.openmrs.module.reporting.serializer.ReportingSerializer;

/**
 * Custom User-Type for storing Mapped objects in a single table within 2 columns.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class MappedDefinitionType implements CompositeUserType<Mapped>, ParameterizedType {

    public static class MappedDefinitionEmbeddable {

        public String definition;

        public String parameterMappings;
    }

    /**
     * Property via ParameterizedType for storing the type of the Mapped Parameterizable.
     */
    private Class<? extends Definition> mappedType;

    @Override
    public Class<?> embeddable() {
        return MappedDefinitionEmbeddable.class;
    }

    @Override
    public Class<Mapped> returnedClass() {
        return Mapped.class;
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public Object getPropertyValue(Mapped component, int property) throws HibernateException {
        if (component == null) {
            return null;
        }
        if (property == 0) {
            Parameterizable parameterizable = component.getParameterizable();
            return parameterizable == null ? null : parameterizable.getUuid();
        }
        if (component.getParameterMappings() == null || component.getParameterMappings().isEmpty()) {
            return null;
        }
        try {
            return Context.getSerializationService()
                    .serialize(component.getParameterMappings(), ReportingSerializer.class);
        }
        catch (Exception e) {
            throw new HibernateException("Unable to serialize mappings for definition", e);
        }
    }

    @Override
    public Mapped instantiate(ValueAccess values) {
        String parameterizableUuid = values.getValue(0, String.class);
        if (StringUtils.isEmpty(parameterizableUuid)) {
            return null;
        }

        String serializedMappings = values.getValue(1, String.class);
        Definition definition = DefinitionContext.getDefinitionByUuid(mappedType, parameterizableUuid);
        Map<String, Object> mappings = new HashMap<String, Object>();
        if (StringUtils.isNotBlank(serializedMappings)) {
            try {
                mappings = Context.getSerializationService()
                        .deserialize(serializedMappings, Map.class, ReportingSerializer.class);
            }
            catch (Exception e) {
                throw new HibernateException("Unable to deserialize parameter mappings for definition", e);
            }
        }
        return new Mapped(definition, mappings);
    }

    @Override
    public Mapped deepCopy(Mapped value) throws HibernateException {
        if (value == null) {
            return null;
        }
        Mapped copy = new Mapped();
        copy.setParameterizable(value.getParameterizable());
        copy.setParameterMappings(new HashMap<String, Object>(value.getParameterMappings()));
        return copy;
    }

    @Override
    public Mapped replace(Mapped original, Mapped target, Object owner) throws HibernateException {
        return original;
    }

    @Override
    public boolean equals(Mapped x, Mapped y) throws HibernateException {
        return x == y || (x != null && x.equals(y));
    }

    @Override
    public int hashCode(Mapped x) throws HibernateException {
        return x == null ? 0 : x.hashCode();
    }

    @Override
    public Serializable disassemble(Mapped value) throws HibernateException {
        return deepCopy(value);
    }

    @Override
    public Mapped assemble(Serializable cached, Object owner) throws HibernateException {
        return cached == null ? null : deepCopy((Mapped) cached);
    }

    @Override
    public void setParameterValues(Properties parameters) {
        String mappedTypeStr = parameters.getProperty("mappedType");
        try {
            mappedType = (Class<? extends Definition>) Context.loadClass(mappedTypeStr);
        }
        catch (Exception e) {
            throw new HibernateException("Error setting the mappedType property to " + mappedTypeStr, e);
        }
    }
}
