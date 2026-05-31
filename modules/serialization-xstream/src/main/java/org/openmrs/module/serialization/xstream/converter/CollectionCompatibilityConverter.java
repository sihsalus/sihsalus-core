/**
 * The contents of this file are subject to the OpenMRS Public License Version 1.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://license.openmrs.org
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>Copyright (C) OpenMRS, LLC. All Rights Reserved.
 */
package org.openmrs.module.serialization.xstream.converter;

import com.thoughtworks.xstream.converters.ConverterLookup;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.hibernate.collection.spi.PersistentCollection;

public class CollectionCompatibilityConverter implements CollectionCompatibility {

  @Override
  public boolean canConvert(Class<?> type) {
    return PersistentCollection.class.isAssignableFrom(type);
  }

  @Override
  public void marshal(
      Object source,
      HierarchicalStreamWriter writer,
      MarshallingContext context,
      ConverterLookup converterLookup) {

    source = compatibilityCopy(source);

    // delegate the collection to the appropriate converter
    converterLookup.lookupConverterForType(source.getClass()).marshal(source, writer, context);
  }

  Object compatibilityCopy(Object source) {
    // Hibernate collection implementations carry session state; copy them to plain JDK collections.
    if (source instanceof SortedMap<?, ?> sortedMap) {
      return new TreeMap<>(sortedMap);
    } else if (source instanceof SortedSet<?> sortedSet) {
      return new TreeSet<>(sortedSet);
    } else if (source instanceof Map<?, ?> map) {
      return new HashMap<>(map);
    } else if (source instanceof Set<?> set) {
      return new HashSet<>(set);
    } else if (source instanceof List<?> list) {
      return new ArrayList<>(list);
    } else if (source instanceof Collection<?> collection) {
      return new ArrayList<>(collection);
    }
    return source;
  }
}
